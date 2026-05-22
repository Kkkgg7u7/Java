CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名称',
    account_type VARCHAR(50) COMMENT '账户类型(微信/支付宝/银行卡/现金)',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    type TINYINT DEFAULT 0 COMMENT '类型(0-支出/1-收入)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

CREATE TABLE IF NOT EXISTS record (
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
