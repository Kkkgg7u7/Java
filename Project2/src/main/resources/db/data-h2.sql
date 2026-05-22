INSERT INTO user (id, username, password, nickname)
SELECT 1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '管理员'
WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 1);

INSERT INTO user (id, username, password, nickname)
SELECT 2, 'test', '098f6bcd4621d373cade4e832627b4f6', '测试用户'
WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 2);

INSERT INTO account (user_id, account_name, account_type, balance)
SELECT 1, '微信钱包', '微信', 1000.00
WHERE NOT EXISTS (SELECT 1 FROM account WHERE user_id = 1 AND account_name = '微信钱包');

INSERT INTO account (user_id, account_name, account_type, balance)
SELECT 1, '支付宝余额', '支付宝', 2000.00
WHERE NOT EXISTS (SELECT 1 FROM account WHERE user_id = 1 AND account_name = '支付宝余额');

INSERT INTO account (user_id, account_name, account_type, balance)
SELECT 1, '工商银行卡', '银行卡', 5000.00
WHERE NOT EXISTS (SELECT 1 FROM account WHERE user_id = 1 AND account_name = '工商银行卡');

INSERT INTO category (user_id, category_name, type)
SELECT 1, '餐饮', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '餐饮' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '交通', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '交通' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '购物', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '购物' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '娱乐', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '娱乐' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '住房', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '住房' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '医疗', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '医疗' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '教育', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '教育' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '其他', 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '其他' AND type = 0);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '工资', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '工资' AND type = 1);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '奖金', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '奖金' AND type = 1);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '投资', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '投资' AND type = 1);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '兼职', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '兼职' AND type = 1);

INSERT INTO category (user_id, category_name, type)
SELECT 1, '其他', 1
WHERE NOT EXISTS (SELECT 1 FROM category WHERE user_id = 1 AND category_name = '其他' AND type = 1);

INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark)
SELECT 1, 0, 35.50, '餐饮', '微信钱包', CURRENT_DATE, '午餐'
WHERE NOT EXISTS (SELECT 1 FROM record WHERE user_id = 1 AND category = '餐饮' AND remark = '午餐');

INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark)
SELECT 1, 0, 15.00, '交通', '支付宝余额', CURRENT_DATE, '地铁'
WHERE NOT EXISTS (SELECT 1 FROM record WHERE user_id = 1 AND category = '交通' AND remark = '地铁');

INSERT INTO record (user_id, type, amount, category, account_type, record_date, remark)
SELECT 1, 1, 8000.00, '工资', '工商银行卡', CURRENT_DATE, '月工资'
WHERE NOT EXISTS (SELECT 1 FROM record WHERE user_id = 1 AND category = '工资' AND remark = '月工资');
