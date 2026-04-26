package com.account.service.impl;

import com.account.dao.CategoryMapper;
import com.account.entity.Category;
import com.account.service.CategoryService;
import com.account.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Category getById(Long id) {
        if (id == null) {
            return null;
        }
        return categoryMapper.selectById(id);
    }

    @Override
    public List<Category> getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return categoryMapper.selectByUserId(userId);
    }

    @Override
    public List<Category> getByUserIdAndType(Long userId, Integer type) {
        if (userId == null) {
            return null;
        }
        return categoryMapper.selectByUserIdAndType(userId, type);
    }

    @Override
    public boolean add(Category category) {
        if (category == null || category.getUserId() == null) {
            return false;
        }
        category.setCreateTime(LocalDateTime.now());
        return categoryMapper.insert(category) > 0;
    }

    @Override
    public boolean update(Category category) {
        if (category == null || category.getId() == null) {
            return false;
        }
        return categoryMapper.update(category) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    public void initDefaultCategories(Long userId) {
        if (userId == null) {
            return;
        }
        for (String name : Constants.DEFAULT_EXPENSE_CATEGORIES) {
            Category category = new Category();
            category.setUserId(userId);
            category.setCategoryName(name);
            category.setType(Constants.CATEGORY_TYPE_EXPENSE);
            category.setCreateTime(LocalDateTime.now());
            categoryMapper.insert(category);
        }
        for (String name : Constants.DEFAULT_INCOME_CATEGORIES) {
            Category category = new Category();
            category.setUserId(userId);
            category.setCategoryName(name);
            category.setType(Constants.CATEGORY_TYPE_INCOME);
            category.setCreateTime(LocalDateTime.now());
            categoryMapper.insert(category);
        }
    }

}
