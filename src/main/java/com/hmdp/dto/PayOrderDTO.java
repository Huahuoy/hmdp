package com.hmdp.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayOrderDTO {

    private Long voucherId;
    private Long id;
    private Integer payType;
}
