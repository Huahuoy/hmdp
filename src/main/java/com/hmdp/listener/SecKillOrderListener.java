package com.hmdp.listener;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;

import com.hmdp.service.IVoucherService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.hmdp.constants.MqConstants.*;

@Component
public class SecKillOrderListener {
    @Autowired
    private IVoucherOrderService vorderService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = SECKILL_QUEUE, durable = "true"),
            exchange = @Exchange(name = SECKILL_EXCHANGE),
            key = SECKILL_KEY
    ))
    @Transactional
    public void secKillOrderListener(VoucherOrder voucherOrder){

        vorderService.save(voucherOrder);
        seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id",voucherOrder.getVoucherId()).update();
    }
}
