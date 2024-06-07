package com.hmdp.listener;



import com.hmdp.dto.PayOrderDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.hmdp.constants.MqConstants.*;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final IVoucherOrderService voucherOrderService;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = DELAY_ORDER_QUEUE_NAME),
            exchange = @Exchange(name = DELAY_EXCHANGE_NAME,delayed = "true"),
            key = DELAY_ORDER_KEY
    ))
    private void ListenPayOrderDelay(Long orderId){

        VoucherOrder order = voucherOrderService.getById(orderId);


        if(order == null || order.getStatus() != 1){
            return;
        }

        voucherOrderService.cancelOrder(orderId);

    }

}
