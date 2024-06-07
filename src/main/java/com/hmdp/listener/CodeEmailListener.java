package com.hmdp.listener;

import com.hmdp.entity.CodeEmail;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.utils.CodeByMail;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.hmdp.constants.MqConstants.*;


@Component
@RequiredArgsConstructor
public class CodeEmailListener {


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = LOGIN_CODE_QUEUE),
            exchange = @Exchange(name = LOGIN_CODE_EXCHANGE,delayed = "true"),
            key = LOGIN_CODE_KEY
    ))
    private void codeEmailListener(CodeEmail codeEmail) throws Exception {
        String code = codeEmail.getCode();
        String target = codeEmail.getTarget();
        CodeByMail.send(target,code);

    }
}
