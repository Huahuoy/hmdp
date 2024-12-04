package com.hmdp.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayOrderDTO {

    private Long voucherId;
    private Long id;
    private String traceNo;
    //Type: 1 余额支付 2 支付宝
    private Integer payType;
}
