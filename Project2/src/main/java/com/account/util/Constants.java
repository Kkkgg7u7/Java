package com.account.util;

public class Constants {

    private Constants() {
    }

    public static final int RECORD_TYPE_EXPENSE = 0;
    public static final int RECORD_TYPE_INCOME = 1;

    public static final int CATEGORY_TYPE_EXPENSE = 0;
    public static final int CATEGORY_TYPE_INCOME = 1;

    public static final String DEFAULT_SALT = "account_book_2024";

    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    public static final int UNAUTHORIZED_CODE = 401;

    public static final String SESSION_USER_KEY = "currentUser";

    public static final String[] DEFAULT_EXPENSE_CATEGORIES = {"餐饮", "交通", "购物", "娱乐", "住房", "医疗", "教育", "其他"};
    public static final String[] DEFAULT_INCOME_CATEGORIES = {"工资", "奖金", "投资", "兼职", "其他"};

    public static final String[] DEFAULT_ACCOUNT_TYPES = {"微信", "支付宝", "银行卡", "现金", "其他"};

}
