CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50),
    balance DECIMAL(10,2) DEFAULT 0.00,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_account_user_id ON account (user_id);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    type TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_category_user_id ON category (user_id);
CREATE INDEX IF NOT EXISTS idx_category_type ON category (type);

CREATE TABLE IF NOT EXISTS record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type TINYINT DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    account_type VARCHAR(50),
    record_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_record_user_id ON record (user_id);
CREATE INDEX IF NOT EXISTS idx_record_type ON record (type);
CREATE INDEX IF NOT EXISTS idx_record_date ON record (record_date);
