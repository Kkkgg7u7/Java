package com.account.dao;

import com.account.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") Long id);

}
