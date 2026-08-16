-- 设置 MySQL 会话级默认字符集，防止客户端连接退化为 latin1
-- 在 docker-entrypoint-initdb.d/ 执行，比 schema.sql 更早生效
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';
SET COLLATION_DATABASE = 'utf8mb4_unicode_ci';
SET COLLATION_SERVER = 'utf8mb4_unicode_ci';
