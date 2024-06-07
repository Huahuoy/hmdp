package com.hmdp.constants;


public class MqConstants {
    public static final String SECKILL_EXCHANGE = "voucher.direct";

    public static final String SECKILL_QUEUE = "seckill.voucher.queue";

    public static final String SECKILL_KEY = "seckill.order";
    public static final String PAY_ORDER_KEY = "pay.order.key";
    public static final String PAY_ORDER_QUEUE_NAME = "pay.order.queue";
    public static final String PAY_ORDER_EXCHANGE = "pay.order.direct";

    public static final String DELAY_EXCHANGE_NAME = "pay.delay.direct";
    public static final String DELAY_ORDER_QUEUE_NAME = "pay.delay.queue";
    public static final String DELAY_ORDER_KEY = "delay.order.pay";
}
