package com.account.service.impl;

import com.account.dao.UserMapper;
import com.account.entity.User;
import com.account.service.UserService;
import com.account.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        if (PasswordUtil.verify(password, user.getPassword())) {
            if (PasswordUtil.needsUpgrade(user.getPassword())) {
                user.setPassword(PasswordUtil.encrypt(password));
                userMapper.update(user);
            }
            return user;
        }
        return null;
    }

    @Override
    public User getById(Long id) {
        if (id == null) {
            return null;
        }
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return userMapper.selectByUsername(username);
    }

    @Override
    public boolean register(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            return false;
        }
        User existUser = userMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            return false;
        }
        user.setPassword(PasswordUtil.encrypt(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean update(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        return userMapper.update(user) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return userMapper.deleteById(id) > 0;
    }

}
