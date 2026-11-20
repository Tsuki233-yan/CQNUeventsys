-- =============================================
-- 通知表扩展：公告发布范围、置顶、发布时间、状态等
-- 适用于已存在 event_database.db 的增量升级（新建库请直接执行 schema.sql）
-- 在 SQLite 中逐条执行；若某列已存在会报错，可跳过该条 ALTER
-- =============================================

ALTER TABLE notification ADD COLUMN publish_scope VARCHAR(10) DEFAULT 'ALL';
ALTER TABLE notification ADD COLUMN is_pinned INTEGER DEFAULT 0;
ALTER TABLE notification ADD COLUMN publish_time DATETIME;
ALTER TABLE notification ADD COLUMN status INTEGER DEFAULT 1;
ALTER TABLE notification ADD COLUMN created_by INTEGER;
ALTER TABLE notification ADD COLUMN update_time DATETIME;
ALTER TABLE notification ADD COLUMN announce_category VARCHAR(20) DEFAULT 'SYSTEM';

-- 历史数据：公告与业务通知补全默认值，保证列表筛选与排序一致
UPDATE notification SET publish_scope = 'ALL' WHERE publish_scope IS NULL;
UPDATE notification SET is_pinned = 0 WHERE is_pinned IS NULL;
UPDATE notification SET publish_time = create_time WHERE publish_time IS NULL;
UPDATE notification SET status = 1 WHERE status IS NULL;
UPDATE notification SET announce_category = 'SYSTEM' WHERE announce_category IS NULL AND type = 1;
UPDATE notification SET status = 1 WHERE type IS NOT NULL AND type <> 1;
