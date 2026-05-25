CREATE DATABASE IF NOT EXISTS account_book DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE account_book;

DROP TABLE IF EXISTS record;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    account_type VARCHAR(50) COMMENT '账户类型(微信/支付宝/银行卡/现金)',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    type TINYINT DEFAULT 0 COMMENT '类型(0-支出/1-收入)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

CREATE TABLE record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT DEFAULT 0 COMMENT '类型(0-支出/1-收入)',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    category VARCHAR(50) COMMENT '分类',
    account_type VARCHAR(50) COMMENT '支付/收款方式',
    record_date DATE COMMENT '发生日期',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账记录表';

INSERT INTO users (username, password, nickname) VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3', '管理员');
INSERT INTO users (username, password, nickname) VALUES ('test', '098f6bcd4621d373cade4e832627b4f6', '测试用户');

INSERT INTO account (user_id, account_name, account_type, balance) VALUES (1, '微信钱包', '微信', 1000.00);
INSERT INTO account (user_id, account_name, account_type, balance) VALUES (1, '支付宝余额', '支付宝', 2000.00);
INSERT INTO account (user_id, account_name, account_type, balance) VALUES (1, '工商银行卡', '银行卡', 5000.00);

INSERT INTO category (user_id, category_name, type) VALUES (1, '餐饮', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '交通', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '购物', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '娱乐', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '住房', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '医疗', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '教育', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '其他', 0);
INSERT INTO category (user_id, category_name, type) VALUES (1, '工资', 1);
INSERT INTO category (user_id, category_name, type) VALUES (1, '奖金', 1);
INSERT INTO category (user_id, category_name, type) VALUES (1, '投资', 1);
INSERT INTO category (user_id, category_name, type) VALUES (1, '兼职', 1);
INSERT INTO category (user_id, category_name, type) VALUES (1, '其他', 1);

INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark) VALUES (1, 0, 35.50, '餐饮', '微信', CURDATE(), '午餐');
INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark) VALUES (1, 0, 15.00, '交通', '支付宝', CURDATE(), '地铁');
INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark) VALUES (1, 1, 8000.00, '工资', '银行卡', CURDATE(), '月工资');
