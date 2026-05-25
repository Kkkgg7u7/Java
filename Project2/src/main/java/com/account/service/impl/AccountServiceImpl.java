package com.account.service.impl;

import com.account.dao.AccountMapper;
import com.account.entity.Account;
import com.account.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public Account getById(Long id) {
        if (id == null) {
            return null;
        }
        return accountMapper.selectById(id);
    }

    @Override
    public Account getByUserIdAndName(Long userId, String accountName) {
        if (userId == null || accountName == null || accountName.trim().isEmpty()) {
            return null;
        }
        return accountMapper.selectByUserIdAndName(userId, accountName);
    }

    @Override
    public List<Account> getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return accountMapper.selectByUserId(userId);
    }

    @Override
    public boolean add(Account account) {
        if (account == null || account.getUserId() == null) {
            return false;
        }
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        account.setCreateTime(LocalDateTime.now());
        return accountMapper.insert(account) > 0;
    }

    @Override
    public boolean update(Account account) {
        if (account == null || account.getId() == null) {
            return false;
        }
        return accountMapper.update(account) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return accountMapper.deleteById(id) > 0;
    }

    @Override
    public BigDecimal getTotalBalance(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = accountMapper.selectTotalBalanceByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public boolean updateBalance(Long id, BigDecimal amount) {
        if (id == null || amount == null) {
            return false;
        }
        return accountMapper.updateBalance(id, amount) > 0;
    }

}
