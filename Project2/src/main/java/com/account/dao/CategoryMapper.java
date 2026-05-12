package com.account.dao;

import com.account.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CategoryMapper {

    Category selectById(@Param("id") Long id);

    List<Category> selectByUserId(@Param("userId") Long userId);

    List<Category> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);

    int deleteByUserId(@Param("userId") Long userId);

}
