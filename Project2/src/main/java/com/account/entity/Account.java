package com.account.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String accountName;

    private String accountType;

    private BigDecimal balance;

    private LocalDateTime createTime;

}
