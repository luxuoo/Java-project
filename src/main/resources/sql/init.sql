-- ============================================
-- 健身房管理系统 数据库初始化脚本 (SQL Server)
-- ============================================

-- 创建数据库
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'gym_db')
BEGIN
    CREATE DATABASE gym_db;
END
GO

USE gym_db;
GO

-- ============================================
-- 1. 系统用户表
-- ============================================
IF OBJECT_ID('sys_user', 'U') IS NOT NULL DROP TABLE sys_user;
CREATE TABLE sys_user (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    username    VARCHAR(50)   NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    real_name   NVARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    avatar      VARCHAR(500),
    gender      TINYINT       DEFAULT 0,
    role        VARCHAR(20)   NOT NULL DEFAULT 'MEMBER',
    status      TINYINT       DEFAULT 1,
    created_at  DATETIME2     DEFAULT GETDATE(),
    updated_at  DATETIME2     DEFAULT GETDATE()
);
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_phone ON sys_user(phone);

-- ============================================
-- 2. 操作日志表
-- ============================================
IF OBJECT_ID('sys_log', 'U') IS NOT NULL DROP TABLE sys_log;
CREATE TABLE sys_log (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT,
    operation   NVARCHAR(200),
    method      VARCHAR(200),
    ip          VARCHAR(50),
    created_at  DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_sys_log_user_id ON sys_log(user_id);

-- ============================================
-- 3. 会员档案表
-- ============================================
IF OBJECT_ID('member', 'U') IS NOT NULL DROP TABLE member;
CREATE TABLE member (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id           BIGINT,
    member_no         VARCHAR(20)   NOT NULL UNIQUE,
    name              NVARCHAR(50)  NOT NULL,
    gender            TINYINT       DEFAULT 0,
    phone             VARCHAR(20),
    id_card           VARCHAR(20),
    birthday          DATE,
    emergency_contact NVARCHAR(50),
    emergency_phone   VARCHAR(20),
    photo             VARCHAR(500),
    health_note       NVARCHAR(500),
    status            TINYINT       DEFAULT 1,
    created_at        DATETIME2     DEFAULT GETDATE(),
    updated_at        DATETIME2     DEFAULT GETDATE()
);
CREATE INDEX idx_member_phone ON member(phone);
CREATE INDEX idx_member_user_id ON member(user_id);

-- ============================================
-- 4. 会员卡/会籍表
-- ============================================
IF OBJECT_ID('membership_card', 'U') IS NOT NULL DROP TABLE membership_card;
CREATE TABLE membership_card (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    member_id       BIGINT        NOT NULL,
    card_type       NVARCHAR(50),
    card_name       NVARCHAR(100),
    price           DECIMAL(10,2),
    start_date      DATE,
    end_date        DATE,
    remaining_times INT,
    status          TINYINT       DEFAULT 0,
    created_at      DATETIME2     DEFAULT GETDATE()
);
CREATE INDEX idx_card_member_id ON membership_card(member_id);

-- ============================================
-- 5. 教练信息表
-- ============================================
IF OBJECT_ID('coach', 'U') IS NOT NULL DROP TABLE coach;
CREATE TABLE coach (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT,
    coach_no        VARCHAR(20)   NOT NULL UNIQUE,
    name            NVARCHAR(50)  NOT NULL,
    phone           VARCHAR(20),
    gender          TINYINT       DEFAULT 0,
    specialty       NVARCHAR(200),
    certification   NVARCHAR(500),
    level           NVARCHAR(50),
    hourly_rate     DECIMAL(10,2),
    bio             NVARCHAR(1000),
    photo           VARCHAR(500),
    status          TINYINT       DEFAULT 1,
    created_at      DATETIME2     DEFAULT GETDATE()
);
CREATE INDEX idx_coach_user_id ON coach(user_id);

-- ============================================
-- 6. 教练排班表
-- ============================================
IF OBJECT_ID('coach_schedule', 'U') IS NOT NULL DROP TABLE coach_schedule;
CREATE TABLE coach_schedule (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    coach_id            BIGINT    NOT NULL,
    schedule_date       DATE      NOT NULL,
    time_slot           VARCHAR(20),
    max_appointment     INT       DEFAULT 1,
    current_appointment INT       DEFAULT 0,
    status              TINYINT   DEFAULT 1
);
CREATE INDEX idx_schedule_coach_date ON coach_schedule(coach_id, schedule_date);

-- ============================================
-- 7. 课程表
-- ============================================
IF OBJECT_ID('course', 'U') IS NOT NULL DROP TABLE course;
CREATE TABLE course (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    type            NVARCHAR(50),
    category        NVARCHAR(50),
    coach_id        BIGINT,
    max_capacity    INT,
    current_count   INT           DEFAULT 0,
    room            NVARCHAR(50),
    start_time      DATETIME2,
    end_time        DATETIME2,
    price           DECIMAL(10,2) DEFAULT 0,
    description     NVARCHAR(500),
    status          TINYINT       DEFAULT 1,
    created_at      DATETIME2     DEFAULT GETDATE()
);
CREATE INDEX idx_course_coach_id ON course(coach_id);
CREATE INDEX idx_course_start_time ON course(start_time);

-- ============================================
-- 8. 课程预约表
-- ============================================
IF OBJECT_ID('course_reservation', 'U') IS NOT NULL DROP TABLE course_reservation;
CREATE TABLE course_reservation (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    course_id     BIGINT    NOT NULL,
    member_id     BIGINT    NOT NULL,
    reserve_time  DATETIME2 DEFAULT GETDATE(),
    status        TINYINT   DEFAULT 1,
    remark        NVARCHAR(200)
);
CREATE INDEX idx_reservation_course ON course_reservation(course_id);
CREATE INDEX idx_reservation_member ON course_reservation(member_id);

-- ============================================
-- 9. 签到记录表
-- ============================================
IF OBJECT_ID('check_in', 'U') IS NOT NULL DROP TABLE check_in;
CREATE TABLE check_in (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    member_id       BIGINT    NOT NULL,
    check_in_time   DATETIME2 DEFAULT GETDATE(),
    check_out_time  DATETIME2,
    duration_min    INT,
    gate            VARCHAR(20)
);
CREATE INDEX idx_checkin_member_time ON check_in(member_id, check_in_time);

-- ============================================
-- 10. 器材设备表
-- ============================================
IF OBJECT_ID('equipment', 'U') IS NOT NULL DROP TABLE equipment;
CREATE TABLE equipment (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    name              NVARCHAR(100) NOT NULL,
    category          NVARCHAR(50),
    brand             NVARCHAR(50),
    model             NVARCHAR(50),
    location          NVARCHAR(50),
    purchase_date     DATE,
    price             DECIMAL(12,2),
    status            TINYINT       DEFAULT 1,
    last_maintenance  DATE,
    next_maintenance  DATE,
    remark            NVARCHAR(500)
);
CREATE INDEX idx_equipment_status ON equipment(status);

-- ============================================
-- 11. 收费/消费记录表
-- ============================================
IF OBJECT_ID('payment', 'U') IS NOT NULL DROP TABLE payment;
CREATE TABLE payment (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    member_id     BIGINT,
    type          NVARCHAR(30),
    amount        DECIMAL(10,2),
    pay_method    NVARCHAR(20),
    related_id    BIGINT,
    remark        NVARCHAR(200),
    operator_id   BIGINT,
    created_at    DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_payment_member ON payment(member_id);
CREATE INDEX idx_payment_created ON payment(created_at);

-- ============================================
-- 12. 通知/消息表
-- ============================================
IF OBJECT_ID('notification', 'U') IS NOT NULL DROP TABLE notification;
CREATE TABLE notification (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT,
    title       NVARCHAR(100),
    content     NVARCHAR(1000),
    type        VARCHAR(20),
    is_read     TINYINT   DEFAULT 0,
    created_at  DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_notification_user ON notification(user_id, is_read);

-- ============================================
-- 初始数据
-- ============================================

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (username, password, real_name, phone, email, gender, role, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'系统管理员', '13800000000', 'admin@gym.com', 1, 'ADMIN', 1);

-- 教练账号 (密码: coach123)
INSERT INTO sys_user (username, password, real_name, phone, email, gender, role, status)
VALUES
('coach01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'王力宏', '13800000001', 'wang@gym.com', 1, 'COACH', 1),
('coach02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'李瑜伽', '13800000002', 'li@gym.com', 2, 'COACH', 1),
('coach03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'张搏击', '13800000003', 'zhang@gym.com', 1, 'COACH', 1);

-- 会员账号 (密码: member123)
INSERT INTO sys_user (username, password, real_name, phone, email, gender, role, status)
VALUES
('member01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'陈小明', '13900000001', 'chen@example.com', 1, 'MEMBER', 1),
('member02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'刘美丽', '13900000002', 'liu@example.com', 2, 'MEMBER', 1),
('member03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'赵强', '13900000003', 'zhao@example.com', 1, 'MEMBER', 1),
('member04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'孙芳', '13900000004', 'sun@example.com', 2, 'MEMBER', 1),
('member05', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', N'周杰', '13900000005', 'zhou@example.com', 1, 'MEMBER', 1);

-- 教练信息
INSERT INTO coach (user_id, coach_no, name, phone, gender, specialty, certification, level, hourly_rate, bio, status)
VALUES
(2, 'C20240001', N'王力宏', '13800000001', 1, N'力量训练,HIIT', N'ACE认证私人教练,NSCA-CPT', N'高级', 300.00, N'10年健身教练经验，擅长增肌减脂', 1),
(3, 'C20240002', N'李瑜伽', '13800000002', 2, N'瑜伽,普拉提', N'RYT-500瑜伽认证', N'高级', 250.00, N'专注瑜伽教学8年，擅长流瑜伽和阴瑜伽', 1),
(4, 'C20240003', N'张搏击', '13800000003', 1, N'搏击,拳击,散打', N'国家一级运动员,搏击教练证', N'明星', 400.00, N'前职业拳击手，教学经验丰富', 1);

-- 会员信息
INSERT INTO member (user_id, member_no, name, gender, phone, birthday, emergency_contact, emergency_phone, health_note, status)
VALUES
(5, 'GM20240001', N'陈小明', 1, '13900000001', '1990-05-15', N'陈大明', '13800001001', N'无特殊病史', 1),
(6, 'GM20240002', N'刘美丽', 2, '13900000002', '1995-08-22', N'刘建军', '13800001002', N'膝盖旧伤，避免高强度跳跃', 1),
(7, 'GM20240003', N'赵强', 1, '13900000003', '1988-12-01', N'赵伟', '13800001003', N'', 1),
(8, 'GM20240004', N'孙芳', 2, '13900000004', '1993-03-10', N'孙建国', '13800001004', N'孕期需注意运动强度', 1),
(9, 'GM20240005', N'周杰', 1, '13900000005', '1992-07-18', N'周华', '13800001005', N'', 1);

-- 会员卡
INSERT INTO membership_card (member_id, card_type, card_name, price, start_date, end_date, remaining_times, status)
VALUES
(1, N'年卡', N'黄金年卡', 3999.00, '2024-01-01', '2025-01-01', NULL, 1),
(2, N'季卡', N'季度畅练卡', 1299.00, '2024-03-01', '2024-06-01', NULL, 2),
(3, N'年卡', N'至尊年卡', 5999.00, '2024-02-01', '2025-02-01', NULL, 1),
(4, N'次卡', N'50次体验卡', 2500.00, '2024-04-01', '2025-04-01', 35, 1),
(5, N'月卡', N'月度畅练卡', 499.00, '2024-05-01', '2024-06-01', NULL, 1);

-- 课程
INSERT INTO course (name, type, category, coach_id, max_capacity, current_count, room, start_time, end_time, price, description, status)
VALUES
(N'晨间瑜伽', N'团课', N'柔韧', 2, 20, 12, N'A101瑜伽室', '2024-06-01 08:00:00', '2024-06-01 09:00:00', 0, N'适合所有级别的晨间瑜伽课程', 1),
(N'HIIT燃脂', N'团课', N'有氧', 1, 15, 10, N'B201训练区', '2024-06-01 10:00:00', '2024-06-01 11:00:00', 0, N'高强度间歇训练，快速燃脂', 1),
(N'搏击入门', N'小班', N'格斗', 3, 8, 6, N'C301搏击馆', '2024-06-01 14:00:00', '2024-06-01 15:30:00', 80.00, N'搏击基础技术教学', 1),
(N'力量训练私教', N'私教', N'力量', 1, 1, 0, N'D101私教区', '2024-06-02 09:00:00', '2024-06-02 10:00:00', 300.00, N'一对一力量训练指导', 1),
(N'普拉提核心', N'团课', N'柔韧', 2, 18, 15, N'A101瑜伽室', '2024-06-02 16:00:00', '2024-06-02 17:00:00', 0, N'核心力量与柔韧性训练', 1),
(N'动感单车', N'团课', N'有氧', 1, 25, 20, N'B202单车房', '2024-06-03 19:00:00', '2024-06-03 20:00:00', 0, N'音乐节奏骑行，畅快燃脂', 1),
(N'拳击进阶', N'小班', N'格斗', 3, 6, 4, N'C301搏击馆', '2024-06-03 15:00:00', '2024-06-03 16:30:00', 120.00, N'拳击组合技术与实战训练', 1),
(N'拉伸放松', N'团课', N'柔韧', 2, 20, 8, N'A101瑜伽室', '2024-06-04 20:00:00', '2024-06-04 20:45:00', 0, N'运动后全身拉伸放松', 1),
(N'功能性训练', N'团课', N'力量', 1, 12, 9, N'B201训练区', '2024-06-05 10:00:00', '2024-06-05 11:00:00', 0, N'提升日常运动能力', 1),
(N'舞蹈健身', N'团课', N'舞蹈', 2, 25, 18, N'A102舞蹈室', '2024-06-06 19:30:00', '2024-06-06 20:30:00', 0, N'拉丁/街舞风格健身舞蹈', 1);

-- 教练排班
INSERT INTO coach_schedule (coach_id, schedule_date, time_slot, max_appointment, current_appointment, status)
VALUES
(1, '2024-06-01', '09:00-10:00', 3, 2, 1),
(1, '2024-06-01', '10:00-11:00', 3, 3, 0),
(1, '2024-06-02', '09:00-10:00', 3, 1, 1),
(2, '2024-06-01', '08:00-09:00', 5, 3, 1),
(2, '2024-06-01', '16:00-17:00', 5, 4, 1),
(2, '2024-06-02', '16:00-17:00', 5, 2, 1),
(3, '2024-06-01', '14:00-15:30', 4, 3, 1),
(3, '2024-06-03', '15:00-16:30', 4, 2, 1);

-- 课程预约
INSERT INTO course_reservation (course_id, member_id, reserve_time, status, remark)
VALUES
(1, 1, '2024-05-28 10:00:00', 2, N''),
(1, 2, '2024-05-28 11:00:00', 1, N''),
(2, 1, '2024-05-29 09:00:00', 1, N''),
(2, 3, '2024-05-29 09:30:00', 1, N''),
(3, 4, '2024-05-30 08:00:00', 1, N'第一次体验搏击'),
(3, 5, '2024-05-30 10:00:00', 1, N''),
(5, 2, '2024-05-31 14:00:00', 2, N''),
(5, 4, '2024-05-31 15:00:00', 1, N''),
(6, 1, '2024-06-01 08:00:00', 1, N''),
(6, 3, '2024-06-01 09:00:00', 1, N'');

-- 签到记录
INSERT INTO check_in (member_id, check_in_time, check_out_time, duration_min, gate)
VALUES
(1, '2024-05-20 08:30:00', '2024-05-20 10:15:00', 105, 'GATE-01'),
(2, '2024-05-20 09:00:00', '2024-05-20 11:00:00', 120, 'GATE-01'),
(3, '2024-05-20 14:00:00', '2024-05-20 16:30:00', 150, 'GATE-02'),
(1, '2024-05-21 07:45:00', '2024-05-21 09:30:00', 105, 'GATE-01'),
(4, '2024-05-21 10:00:00', '2024-05-21 11:30:00', 90, 'GATE-01'),
(5, '2024-05-21 15:00:00', '2024-05-21 17:00:00', 120, 'GATE-02'),
(2, '2024-05-22 08:00:00', '2024-05-22 09:45:00', 105, 'GATE-01'),
(3, '2024-05-22 16:00:00', '2024-05-22 18:00:00', 120, 'GATE-02'),
(1, '2024-05-23 09:00:00', '2024-05-23 11:00:00', 120, 'GATE-01'),
(5, '2024-05-23 14:00:00', '2024-05-23 15:30:00', 90, 'GATE-01'),
(4, '2024-05-24 08:30:00', '2024-05-24 10:00:00', 90, 'GATE-02'),
(2, '2024-05-24 10:00:00', '2024-05-24 12:00:00', 120, 'GATE-01'),
(1, '2024-05-25 07:30:00', '2024-05-25 09:00:00', 90, 'GATE-01'),
(3, '2024-05-25 15:00:00', '2024-05-25 17:30:00', 150, 'GATE-02'),
(5, '2024-05-26 09:00:00', '2024-05-26 10:30:00', 90, 'GATE-01'),
(4, '2024-05-26 14:00:00', '2024-05-26 15:45:00', 105, 'GATE-02'),
(1, '2024-05-27 08:00:00', '2024-05-27 10:00:00', 120, 'GATE-01'),
(2, '2024-05-27 09:30:00', '2024-05-27 11:00:00', 90, 'GATE-01'),
(3, '2024-05-28 16:00:00', '2024-05-28 18:00:00', 120, 'GATE-02'),
(5, '2024-05-28 10:00:00', '2024-05-28 11:30:00', 90, 'GATE-01');

-- 支付记录
INSERT INTO payment (member_id, type, amount, pay_method, related_id, remark, operator_id, created_at)
VALUES
(1, N'购卡', 3999.00, N'微信', 1, N'购买黄金年卡', 1, '2024-01-01 10:00:00'),
(2, N'购卡', 1299.00, N'支付宝', 2, N'购买季度畅练卡', 1, '2024-03-01 14:00:00'),
(3, N'购卡', 5999.00, N'银行卡', 3, N'购买至尊年卡', 1, '2024-02-01 09:00:00'),
(4, N'购卡', 2500.00, N'现金', 4, N'购买50次体验卡', 1, '2024-04-01 11:00:00'),
(5, N'购卡', 499.00, N'微信', 5, N'购买月度畅练卡', 1, '2024-05-01 16:00:00'),
(4, N'私教', 300.00, N'微信', NULL, N'力量训练私教课', 1, '2024-05-15 10:00:00'),
(1, N'课程', 80.00, N'支付宝', 3, N'搏击入门课程', 1, '2024-05-20 14:00:00'),
(3, N'课程', 120.00, N'微信', 7, N'拳击进阶课程', 1, '2024-05-22 15:00:00');

-- 器材设备
INSERT INTO equipment (name, category, brand, model, location, purchase_date, price, status, last_maintenance, next_maintenance, remark)
VALUES
(N'跑步机T1', N'有氧', N'Life Fitness', N'T9i', N'A区有氧区', '2023-01-15', 45000.00, 1, '2024-03-01', '2024-06-01', N''),
(N'跑步机T2', N'有氧', N'Life Fitness', N'T9i', N'A区有氧区', '2023-01-15', 45000.00, 1, '2024-03-01', '2024-06-01', N''),
(N'椭圆机E1', N'有氧', N'Precor', N'EFX885', N'A区有氧区', '2023-03-20', 38000.00, 1, '2024-02-15', '2024-05-15', N''),
(N'史密斯架S1', N'力量', N'Hammer Strength', N'MTSM', N'B区力量区', '2022-06-01', 28000.00, 1, '2024-01-10', '2024-07-10', N''),
(N'龙门架D1', N'力量', N'Cybex', N'Bravo', N'B区力量区', '2022-06-01', 35000.00, 2, '2024-04-01', '2024-05-01', N'滑轮需要维修'),
(N'哑铃套装', N'自由重量', N'Rogue', N'Urethane', N'C区自由重量区', '2023-05-10', 25000.00, 1, '2024-04-01', '2024-07-01', N'2.5kg-50kg全套'),
(N'壶铃套装', N'功能训练', N'Kettlebell Kings', N'Set', N'D区功能训练区', '2023-08-15', 8000.00, 1, '2024-03-01', '2024-09-01', N'4kg-32kg'),
(N'动感单车B1', N'有氧', N'Star Trac', N'Blade', N'A202单车房', '2023-02-01', 12000.00, 3, '2024-01-01', '2024-04-01', N'已停用，等待更换'),
(N'划船机R1', N'有氧', N'Concept2', N'Model D', N'A区有氧区', '2023-06-20', 9500.00, 1, '2024-04-01', '2024-07-01', N''),
(N'战绳', N'功能训练', N'Onnit', N'Battle Rope', N'D区功能训练区', '2023-09-01', 1500.00, 1, '2024-03-01', '2024-09-01', N'15米');

-- 通知消息
INSERT INTO notification (user_id, title, content, type, is_read, created_at)
VALUES
(5, N'欢迎加入', N'欢迎陈小明成为我们的会员！祝您健身愉快！', N'SYSTEM', 1, '2024-01-01 10:05:00'),
(6, N'欢迎加入', N'欢迎刘美丽成为我们的会员！祝您健身愉快！', N'SYSTEM', 1, '2024-03-01 14:05:00'),
(5, N'会员卡到期提醒', N'您的黄金年卡将于2025年1月1日到期，请及时续费。', N'REMINDER', 0, '2024-12-01 09:00:00'),
(6, N'会员卡已过期', N'您的季度畅练卡已过期，欢迎续费继续使用。', N'REMINDER', 0, '2024-06-02 09:00:00'),
(5, N'新课程推荐', N'搏击入门课程火热报名中，快来体验吧！', N'PROMOTION', 0, '2024-05-28 10:00:00'),
(7, N'新课程推荐', N'拳击进阶课程即将开班，名额有限！', N'PROMOTION', 0, '2024-05-28 10:00:00'),
(1, N'系统维护通知', N'系统将于本周日凌晨2:00-4:00进行维护升级。', N'SYSTEM', 0, '2024-05-29 09:00:00');

PRINT 'Database initialization completed successfully!';
GO
