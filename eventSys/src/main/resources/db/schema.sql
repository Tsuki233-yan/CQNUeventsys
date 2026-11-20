-- 校园活动综合管理系统 数据库初始化脚本 (SQLite)

-- =============================================
-- 1. 用户表 (user) - 扩展校园信息字段（重庆师范大学场景）
-- 说明：college 存储重庆师范大学真实学院名（计算机学院、文学院、数学科学学院、外国语学院、
--       教育科学学院、历史与社会学院、新闻与传媒学院、音乐学院、美术学院、体育与健康科学学院、
--       物理与电子工程学院、化学学院、生命科学学院、地理与旅游学院、经济与管理学院、
--       马克思主义学院、初等教育学院、特殊教育学院等），种子数据见 data.sql
-- =============================================
CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    email VARCHAR(100),
    phone VARCHAR(20),
    real_name VARCHAR(50),
    user_type INTEGER DEFAULT 0, -- 用户身份：0-学生，1-教师（NULL 视为未绑定身份的老账号，需在个人中心补填；管理员 role=1 豁免绑定校验）
    student_id VARCHAR(50),      -- 学号：学生为10位数字（前4位为入学年份2020~2026），教师为NULL【模拟校验规则，可替换为学籍接口对接】
    teacher_id VARCHAR(50),      -- 工号：教师为6位数字，学生为NULL【模拟校验规则，可替换为学籍（教工）接口对接】
    college VARCHAR(100),        -- 学院（重庆师范大学真实学院名，见文件头说明与 data.sql）
    major VARCHAR(100),
    grade VARCHAR(20),
    gender INTEGER DEFAULT 0,
    bio TEXT,
    role INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    organized_count INTEGER DEFAULT 0,
    participated_count INTEGER DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0
);

-- =============================================
-- 2. 活动分类表 (category)
-- =============================================
CREATE TABLE IF NOT EXISTS category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    icon VARCHAR(200),
    sort_order INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    activity_count INTEGER DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0
);

-- =============================================
-- 3. 活动表 (activity) - 扩展字段
-- 状态说明：-2-已下架, -1-已拒绝, 0-草稿, 1-待审核, 2-报名中, 3-即将开始, 4-进行中, 5-已结束
-- =============================================
CREATE TABLE IF NOT EXISTS activity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    organizer_id INTEGER NOT NULL,
    location VARCHAR(200),
    start_time DATETIME,
    end_time DATETIME,
    registration_start_time DATETIME,
    registration_end_time DATETIME,
    max_participants INTEGER DEFAULT 0,
    current_participants INTEGER DEFAULT 0,
    tags VARCHAR(200),
    status INTEGER DEFAULT 1,
    category_id INTEGER,
    need_review INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    avg_rating DECIMAL(2,1) DEFAULT NULL,
    review_count INTEGER DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (organizer_id) REFERENCES user(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- =============================================
-- 4. 活动报名表 (activity_registration) - 扩展审核字段和角色
-- =============================================
CREATE TABLE IF NOT EXISTS activity_registration (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    role INTEGER DEFAULT 2,
    status INTEGER DEFAULT 0,
    remark TEXT,
    images TEXT,
    reviewer_id INTEGER,
    review_time DATETIME,
    review_remark TEXT,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (reviewer_id) REFERENCES user(id),
    UNIQUE(activity_id, user_id)
);

-- =============================================
-- 5. 活动成果表 (activity_result)
-- 活动结束后由组织者或管理者创建的成果展示贴
-- =============================================
CREATE TABLE IF NOT EXISTS activity_result (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    images TEXT,
    creator_id INTEGER NOT NULL,
    view_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (creator_id) REFERENCES user(id)
);

-- =============================================
-- 6. 评论表 (comment)
-- 支持对活动(activity)和成果展示贴(result)进行评论
-- target_type: 1-活动, 2-成果展示贴
-- =============================================
CREATE TABLE IF NOT EXISTS comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_id INTEGER NOT NULL,
    target_type INTEGER DEFAULT 1 NOT NULL,
    user_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    parent_id INTEGER DEFAULT 0,
    reply_to_user_id INTEGER DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- =============================================
-- 7. 活动收藏表 (activity_favorite)
-- =============================================
CREATE TABLE IF NOT EXISTS activity_favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    create_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE(activity_id, user_id)
);

-- =============================================
-- 8. 活动签到记录表 (check_in_record)
-- =============================================
CREATE TABLE IF NOT EXISTS check_in_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    registration_id INTEGER NOT NULL,
    check_in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    check_in_type INTEGER DEFAULT 1,
    location VARCHAR(200),
    remark TEXT,
    create_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (registration_id) REFERENCES activity_registration(id)
);

-- =============================================
-- 9. 系统通知表 (notification)
-- =============================================
CREATE TABLE IF NOT EXISTS notification (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type INTEGER DEFAULT 1,
    activity_id INTEGER,
    is_read INTEGER DEFAULT 0,
    read_time DATETIME,
    create_time DATETIME,
    deleted INTEGER DEFAULT 0,
    publish_scope VARCHAR(10) DEFAULT 'ALL',
    is_pinned INTEGER DEFAULT 0,
    publish_time DATETIME,
    status INTEGER DEFAULT 1,
    created_by INTEGER,
    update_time DATETIME,
    announce_category VARCHAR(20) DEFAULT 'SYSTEM',
    FOREIGN KEY (activity_id) REFERENCES activity(id)
);

-- =============================================
-- 10. 活动审核记录表 (activity_audit)
-- =============================================
CREATE TABLE IF NOT EXISTS activity_audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL,
    audit_type INTEGER DEFAULT 1,
    status INTEGER DEFAULT 0,
    submitter_id INTEGER NOT NULL,
    submit_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    auditor_id INTEGER,
    audit_time DATETIME,
    audit_opinion TEXT,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (submitter_id) REFERENCES user(id),
    FOREIGN KEY (auditor_id) REFERENCES user(id)
);

-- =============================================
-- 11. 活动评价表 (activity_review)
-- 每个用户对同一活动只能评价一次（唯一索引保证）
-- =============================================
CREATE TABLE IF NOT EXISTS activity_review (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    rating INTEGER NOT NULL,
    content TEXT,
    reply TEXT,
    create_time DATETIME,
    update_time DATETIME,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE(activity_id, user_id)
);

-- =============================================
-- 创建索引优化查询性能
-- =============================================

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_user_username ON user(username);
CREATE INDEX IF NOT EXISTS idx_user_student_id ON user(student_id);
CREATE INDEX IF NOT EXISTS idx_user_teacher_id ON user(teacher_id);
CREATE INDEX IF NOT EXISTS idx_user_user_type ON user(user_type);
CREATE INDEX IF NOT EXISTS idx_user_college ON user(college);
CREATE INDEX IF NOT EXISTS idx_user_role ON user(role);
CREATE INDEX IF NOT EXISTS idx_user_status ON user(status);

-- 活动分类表索引
CREATE INDEX IF NOT EXISTS idx_category_status ON category(status);
CREATE INDEX IF NOT EXISTS idx_category_sort ON category(sort_order);

-- 活动表索引
CREATE INDEX IF NOT EXISTS idx_activity_organizer ON activity(organizer_id);
CREATE INDEX IF NOT EXISTS idx_activity_category ON activity(category_id);
CREATE INDEX IF NOT EXISTS idx_activity_status ON activity(status);
CREATE INDEX IF NOT EXISTS idx_activity_start_time ON activity(start_time);

-- 活动报名表索引
CREATE INDEX IF NOT EXISTS idx_reg_activity ON activity_registration(activity_id);
CREATE INDEX IF NOT EXISTS idx_reg_user ON activity_registration(user_id);
CREATE INDEX IF NOT EXISTS idx_reg_status ON activity_registration(status);

-- 活动成果表索引
CREATE INDEX IF NOT EXISTS idx_result_activity ON activity_result(activity_id);

-- 评论表索引
CREATE INDEX IF NOT EXISTS idx_comment_target ON comment(target_id, target_type);
CREATE INDEX IF NOT EXISTS idx_comment_user ON comment(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON comment(parent_id);

-- 活动收藏表索引
CREATE INDEX IF NOT EXISTS idx_fav_activity ON activity_favorite(activity_id);
CREATE INDEX IF NOT EXISTS idx_fav_user ON activity_favorite(user_id);

-- 签到记录表索引
CREATE INDEX IF NOT EXISTS idx_checkin_activity ON check_in_record(activity_id);
CREATE INDEX IF NOT EXISTS idx_checkin_user ON check_in_record(user_id);
CREATE INDEX IF NOT EXISTS idx_checkin_reg ON check_in_record(registration_id);

-- 通知表索引
CREATE INDEX IF NOT EXISTS idx_notif_user ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_type ON notification(type);
CREATE INDEX IF NOT EXISTS idx_notif_read ON notification(is_read);

-- 活动审核表索引
CREATE INDEX IF NOT EXISTS idx_audit_activity ON activity_audit(activity_id);
CREATE INDEX IF NOT EXISTS idx_audit_status ON activity_audit(status);
CREATE INDEX IF NOT EXISTS idx_audit_submitter ON activity_audit(submitter_id);

-- 活动评价表索引
CREATE INDEX IF NOT EXISTS idx_review_activity ON activity_review(activity_id);
CREATE INDEX IF NOT EXISTS idx_review_user ON activity_review(user_id);
