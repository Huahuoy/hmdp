package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RabbitMqHelper;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.hmdp.constants.MqConstants.SECKILL_EXCHANGE;
import static com.hmdp.constants.MqConstants.SECKILL_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private RedisIdWorker idWorker;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate redisTemplatel;

    @Autowired
    private RabbitMqHelper rabbitMqHelper;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    @Override
    public Result seckillVocher(Long voucherId) {
        LocalDateTime now = LocalDateTime.now();

        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if(seckillVoucher == null){
            return Result.fail("优惠券不存在");
        }
        if(seckillVoucher.getBeginTime().isAfter(now)){
            return Result.fail("抢购还未开始！");
        }
        if(seckillVoucher.getEndTime().isBefore(now)){
            return Result.fail("抢购已结束！");
        }
        Long userId = UserHolder.getUser().getId();

        int result = redisTemplatel.execute(
                SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString()
        ).intValue();

        if(result != 0){
            return Result.fail(result == 1 ? "库存不足！":"请勿重复下单");
        }
        long id = idWorker.nextId("secKillOrder");
        //TODO rabbitmq实现异步到数据库下单功能
        //createOrderWithMq(userId,voucherId,id);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(id);
        voucherOrder.setUserId(userId);
        rabbitMqHelper.sendMessage(
                SECKILL_EXCHANGE,SECKILL_KEY,voucherOrder
        );
        return Result.ok(id);
    }

    private void createOrderWithMq(Long userId, Long voucherId,long id) {

        //VoucherOrder vo = VoucherOrder.builder().id(id).voucherId(voucherId).userId(userId).build();
        //save(vo);

    }

//    @Transactional
//    public Result createOrderId(Long voucherId){
//        Long userId = UserHolder.getUser().getId();
//        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//        if(count > 0){
//            return Result.fail("您已购买过！请勿重复购买");
//        }
//        boolean isUpdate = seckillVoucherService.update()
//                .setSql("stock = stock - 1").eq("voucher_id", voucherId)
//                .gt("stock", 0).update();
//        if(!isUpdate){
//            return Result.fail("库存不足！");
//        }
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = idWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }
}
