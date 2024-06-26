package com.hmdp.service;

import com.hmdp.dto.PayOrderDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVocher(Long voucherId);


    Result pay(PayOrderDTO dto);

    void markOrderPaySuccess(PayOrderDTO dto);

    void cancelOrder(Long orderId);
}
