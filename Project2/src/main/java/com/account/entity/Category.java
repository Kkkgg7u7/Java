package com.account.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String categoryName;

    private Integer type;

    private LocalDateTime createTime;

}
