package com.account.service;

import com.account.entity.User;

public interface UserService {

    User login(String username, String password);

    User getById(Long id);

    User getByUsername(String username);

    boolean register(User user);

    boolean update(User user);

    boolean delete(Long id);

}
