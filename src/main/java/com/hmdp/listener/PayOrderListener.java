package com.hmdp.listener;

import com.hmdp.dto.PayOrderDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.hmdp.constants.MqConstants.*;

@Component
public class PayOrderListener {

    @Autowired
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = PAY_ORDER_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = PAY_ORDER_EXCHANGE),
            key = PAY_ORDER_KEY
    ))
    @Transactional
    public void secKillOrderListener(PayOrderDTO dto){
        voucherOrderService.markOrderPaySuccess(dto);
    }
}
