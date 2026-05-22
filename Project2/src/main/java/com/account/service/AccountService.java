package com.account.service;

import com.account.entity.Account;
import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    Account getById(Long id);

    Account getByUserIdAndName(Long userId, String accountName);

    List<Account> getByUserId(Long userId);

    boolean add(Account account);

    boolean update(Account account);

    boolean delete(Long id);

    BigDecimal getTotalBalance(Long userId);

    boolean updateBalance(Long id, BigDecimal amount);

}
