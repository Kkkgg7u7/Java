package com.account.service;

import com.account.entity.Category;
import java.util.List;

public interface CategoryService {

    Category getById(Long id);

    List<Category> getByUserId(Long userId);

    List<Category> getByUserIdAndType(Long userId, Integer type);

    boolean add(Category category);

    boolean update(Category category);

    boolean delete(Long id);

    void initDefaultCategories(Long userId);

}
