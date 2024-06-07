package com.hmdp.controller;


import com.hmdp.dto.PayOrderDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return voucherOrderService.seckillVocher(voucherId);
    }

    @PostMapping("/pay")
    public Result payVoucher(@RequestBody PayOrderDTO dto){
        return voucherOrderService.pay(dto);
    }
}
