-- ASCENTROAM 云端 · 邮箱验证码
--
-- 注册验证与找回密码共用一张表，用 purpose 区分。
--
-- 设计要点：
--  1. **只存 HMAC，不存明文**。6 位数字只有 100 万种，裸 sha256 瞬间就能反查，
--     所以必须带 pepper（服务端密钥）。pepper 不在库里，库泄露也拿不到码。
--  2. 记录 attempts 而不是靠客户端限次：验证码是唯一能绕过密码的东西，
--     次数必须由服务端扣。
--  3. consumed_at 一旦写入就不可重放，避免同一封邮件被用两次。

create table if not exists ascent_email_codes (
  id          uuid   primary key default gen_random_uuid(),
  email       text   not null,
  purpose     text   not null,          -- 'register' | 'reset'
  code_hash   text   not null,
  expires_at  bigint not null,
  attempts    int    not null default 0,
  consumed_at bigint,
  created_at  bigint not null
);

-- 查询形状固定为「某邮箱 + 某用途，按时间倒序取最新一条」
create index if not exists ix_codes_lookup
  on ascent_email_codes (email, purpose, created_at desc);

-- 清理旧码用得上
create index if not exists ix_codes_created on ascent_email_codes (created_at);

alter table ascent_email_codes enable row level security;
