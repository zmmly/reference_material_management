-- 添加密码修改标记字段
-- 执行此脚本为现有用户添加 password_changed 字段

ALTER TABLE `user` ADD COLUMN `password_changed` TINYINT(1) DEFAULT 0 COMMENT '是否已修改密码' AFTER `status`;

-- 将管理员用户标记为已修改密码（如果已经手动修改过）
-- UPDATE `user` SET `password_changed` = 1 WHERE `username` = 'admin';
