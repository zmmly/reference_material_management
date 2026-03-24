-- 为 supplier 表添加证件照片字段
-- 执行时间：2026-03-24

ALTER TABLE `supplier`
ADD COLUMN `certificate_images` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
COMMENT '证件照片路径（JSON数组）' AFTER `address`;
