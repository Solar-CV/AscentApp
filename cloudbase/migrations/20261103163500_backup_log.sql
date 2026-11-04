-- ASCENT 云端 · 备份运行日志
--
-- 备份函数每次运行写一行。存在的意义只有一个：**能发现「备份悄悄坏了」**。
-- 没有这张表，备份连续失败一个月也不会有人知道，直到真出事才发现一个都恢复不了。
--
-- status: running → ok / failed。只有 ok 且 object_key 非空才算真的成功。

create table if not exists ascent_backup_log (
  id         bigserial primary key,
  started_at bigint  not null,
  finished_at bigint,
  status     text    not null default 'running',
  object_key text,
  bytes      bigint,
  -- 每表行数快照，恢复前拿它核对，防止「备份成功了但其实是空的」
  tables     jsonb,
  error      text
);

create index if not exists ix_backup_log_started on ascent_backup_log (started_at desc);

alter table ascent_backup_log enable row level security;
