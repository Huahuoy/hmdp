package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hmdp.controller.PayController;
import com.hmdp.dto.PayOrderDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.*;
import com.hmdp.mapper.PocketMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

import static com.hmdp.constants.MqConstants.*;

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
    private PayController payController;
    @Autowired
    private IVoucherService voucherService;

    @Autowired
    private StringRedisTemplate redisTemplatel;

    @Autowired
    private RabbitMqHelper rabbitMqHelper;

    @Autowired
    private PocketMapper pocketMapper;
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;
    @Autowired
    private IUserService userService;
    @Autowired
    private IPocketService pocketService;

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
        //异步监听支付超时功能
        rabbitMqHelper.sendDelayMessage(
                DELAY_EXCHANGE_NAME,DELAY_ORDER_KEY,id,900000);
        return Result.ok(id);
    }

    @Override
    public Result pay(PayOrderDTO dto) {
        Long userId = UserHolder.getUser().getId();

        Long voucherId = dto.getVoucherId();
        Voucher voucher = voucherService.getById(voucherId);
        VoucherOrder order = getById(dto.getId());
        if(order.getStatus() != 1){
            return Result.fail("订单状态有误");
        }
        if(dto.getPayType() != 1 && dto.getPayType() != 2){
            return Result.fail("支付方式有误");
        }
        if(dto.getPayType() == 1) {
            Pocket pocket = pocketMapper.getOnePocket(userId);

            if (pocket == null) {
                return Result.fail("钱包信息错误");
            }
            if (pocket.getCheck() < voucher.getPayValue()) {
                return Result.fail("余额不足");
            }

            pocketMapper.pay(userId, voucher.getPayValue());

            //异步通知修改订单状态
            rabbitMqHelper.sendMessageWithConfirm(PAY_ORDER_EXCHANGE, PAY_ORDER_KEY, dto, 5);


        }else {//支付宝支付
            AliPay aliPay = new AliPay();
            aliPay.setTotalAmount(voucher.getPayValue());
            aliPay.setSubject(voucher.getTitle());
            aliPay.setTraceNo(String.valueOf(dto.getId()));
            try {
                payController.pay(aliPay);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        return Result.ok();

    }

    @Override
    public void markOrderPaySuccess(PayOrderDTO dto) {

        VoucherOrder voucherOrder = BeanUtil.copyProperties(dto, VoucherOrder.class);
        voucherOrder.setPayTime(LocalDateTime.now());
        voucherOrder.setStatus(2);
        voucherOrderMapper.markOrderPaySuccess(voucherOrder);

    }

    @Override
    public void cancelOrder(Long orderId) {
        VoucherOrder order = getById(orderId);
        if(order.getCreateTime().plusMinutes(15).isBefore(LocalDateTime.now())){
            order.setStatus(4);
            updateById(order);
        }
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
