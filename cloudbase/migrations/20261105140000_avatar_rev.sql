-- ASCENTROAM 云端 · 头像版本号改为单调递增
--
-- 问题：原来 PUT /avatar 用 users.avatar_version + 1，DELETE 把它归零。
-- 于是「删头像 → 再传一张」版本号会从 1 重新开始，而别的设备记着 cloudAvatarVersion=5，
-- 判断 remoteVersion(1) <= 5 就永远不下载 —— 那台设备会一直显示旧头像。
--
-- 修法：新增 avatar_rev 作为只增不减的计数器；avatar_version 仍表示「当前头像的版本，0=没有」。
-- 删除只清 avatar_version，不动 avatar_rev，下一次上传继续往上加。

alter table ascent_users
  add column if not exists avatar_rev bigint not null default 0;

-- 存量回填：已有头像的账号，rev 从当前 version 起步
update ascent_users
   set avatar_rev = avatar_version
 where avatar_rev = 0
   and avatar_version > 0;
