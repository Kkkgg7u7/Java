package com.account.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Record implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Integer type;

    private BigDecimal amount;

    private String category;

    private String accountType;

    private LocalDate recordDate;

    private String remark;

    private LocalDateTime createTime;

}
