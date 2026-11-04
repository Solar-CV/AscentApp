-- ASCENT 云端 · 初始表结构
--
-- 对应 CLOUD-PLAN.md §4。时间一律存 bigint epoch 毫秒：Node 侧 Date.now() 直接落库，
-- 不用处理时区，也不依赖数据库的 now()。
--
-- 约定：所有表都开 RLS 且不建任何 policy。云函数用服务端 API Key（service_role）
-- 访问，天然绕过 RLS；anon / authenticated 什么都读不到。
-- 客户端从不直连数据库，这条是纯防御。

create table if not exists ascent_users (
  id                uuid primary key default gen_random_uuid(),
  email             text,
  email_verified    boolean not null default false,
  status            text    not null default 'active',
  nickname          text    not null default '',
  avatar_version    int     not null default 0,
  created_at        bigint  not null,
  updated_at        bigint  not null,
  login_fails       int     not null default 0,
  login_fail_window bigint  not null default 0
);

create unique index if not exists ux_users_email
  on ascent_users (email) where email is not null and email <> '';

create table if not exists ascent_identities (
  id         uuid   primary key default gen_random_uuid(),
  user_id    uuid   not null references ascent_users(id) on delete cascade,
  provider   text   not null,
  subject    text   not null,
  created_at bigint not null
);

create unique index if not exists ux_identities_provider_subject
  on ascent_identities (provider, subject);

create index if not exists ix_identities_user on ascent_identities (user_id);

create table if not exists ascent_credentials (
  user_id       uuid primary key references ascent_users(id) on delete cascade,
  password_hash text   not null,
  updated_at    bigint not null
);

create table if not exists ascent_sessions (
  id                 uuid    primary key default gen_random_uuid(),
  user_id            uuid    not null references ascent_users(id) on delete cascade,
  access_hash        text    not null,
  access_expires_at  bigint  not null,
  refresh_hash       text    not null,
  refresh_expires_at bigint  not null,
  device_id          text    not null default '',
  platform           text    not null default '',
  app_version        text    not null default '',
  created_at         bigint  not null,
  last_used_at       bigint  not null,
  revoked            boolean not null default false
);

create unique index if not exists ux_sessions_access  on ascent_sessions (access_hash);
create unique index if not exists ux_sessions_refresh on ascent_sessions (refresh_hash);
create index        if not exists ix_sessions_user    on ascent_sessions (user_id);

create table if not exists ascent_states (
  user_id       uuid  primary key references ascent_users(id) on delete cascade,
  rev           int   not null default 0,
  payload       jsonb not null,
  payload_bytes int   not null default 0,
  updated_at    bigint not null
);

create table if not exists ascent_avatars (
  user_id    uuid  primary key references ascent_users(id) on delete cascade,
  data       text  not null,
  version    int   not null default 1,
  updated_at bigint not null
);

alter table ascent_users       enable row level security;
alter table ascent_identities  enable row level security;
alter table ascent_credentials enable row level security;
alter table ascent_sessions    enable row level security;
alter table ascent_states      enable row level security;
alter table ascent_avatars     enable row level security;
