package com.account.dao;

import com.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountMapper {

    Account selectById(@Param("id") Long id);

    List<Account> selectByUserId(@Param("userId") Long userId);

    int insert(Account account);

    int update(Account account);

    int deleteById(@Param("id") Long id);

    int deleteByUserId(@Param("userId") Long userId);

    BigDecimal selectTotalBalanceByUserId(@Param("userId") Long userId);

    int updateBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

}
