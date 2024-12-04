package com.hmdp.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.qrcode.QrCodeUtil;


import com.alibaba.fastjson.JSONObject;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.hmdp.dto.PayOrderDTO;
import com.hmdp.entity.AliPay;
import com.hmdp.service.FileStorageService;
import com.hmdp.utils.RabbitMqHelper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.hmdp.constants.MqConstants.PAY_ORDER_EXCHANGE;
import static com.hmdp.constants.MqConstants.PAY_ORDER_KEY;

@RestController
@Slf4j
@RequestMapping("/alipay")
public class PayController {
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RabbitMqHelper rabbitMqHelper;
    @Resource
    private Config config;
    /**
     * 收银台点击结账
     * 发起下单请求
     */
    @GetMapping("/pay")
    public void pay(AliPay aliPay) throws Exception {
        System.out.println(config.notifyUrl);
        Factory.setOptions(config);
        //调用支付宝的接口
        AlipayTradePrecreateResponse payResponse = Factory.Payment.FaceToFace().preCreate(aliPay.getSubject(), aliPay.getTraceNo(), String.valueOf(aliPay.getTotalAmount()));
        //参照官方文档响应示例，解析返回结果
        String httpBodyStr = payResponse.getHttpBody();
        JSONObject jsonObject = JSONObject.parseObject(httpBodyStr);
        String qrUrl = jsonObject.getJSONObject("alipay_trade_precreate_response").get("qr_code").toString();
        BufferedImage image = QrCodeUtil.generate(qrUrl, 300, 300);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // 将BufferedImage以指定格式（这里是PNG）写入到ByteArrayOutputStream
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        // 获取字节数组
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        fileStorageService.uploadImgFile("alipay",UUID.randomUUID().toString(),new ByteArrayInputStream(imageBytes));

    }
    /**
     * 给支付宝的回调接口
     */
    @PostMapping("/notify")
    public void notify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> params = new HashMap<>();
        //获取支付宝POST过来反馈信息，将异步通知中收到的待验证所有参数都存放到map中
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (String name : parameterMap.keySet()) {
            String[] values = parameterMap.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决
            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        //验签
        Boolean signResult = Factory.Payment.Common().verifyNotify(params);
        if (signResult) {
            log.info("收到支付宝发送的支付结果通知");
            String out_trade_no = request.getParameter("out_trade_no");
            log.info("交易流水号：{}", out_trade_no);
            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");
            //交易成功
            switch (trade_status) {
                case "TRADE_SUCCESS":
                    //支付成功的业务逻辑，比如落库，开vip权限等
                    log.info("订单：{} 交易成功", out_trade_no);
                    PayOrderDTO payOrderDTO = new PayOrderDTO();
                    payOrderDTO.setPayType(2);
                    payOrderDTO.setTraceNo(out_trade_no);

                    //异步通知修改订单状态
                    rabbitMqHelper.sendMessageWithConfirm(PAY_ORDER_EXCHANGE, PAY_ORDER_KEY, payOrderDTO, 5);

                    break;
                case "TRADE_FINISHED":
                    log.info("交易结束，不可退款");
                    //其余业务逻辑
                    break;
                case "TRADE_CLOSED":
                    log.info("超时未支付，交易已关闭，或支付完成后全额退款");
                    //其余业务逻辑
                    break;
                case "WAIT_BUYER_PAY":
                    log.info("交易创建，等待买家付款");
                    //其余业务逻辑
                    break;
            }
            response.getWriter().write("success");   //返回success给支付宝，表示消息我已收到，不用重调

        } else {
            response.getWriter().write("fail");   ///返回fail给支付宝，表示消息我没收到，请重试
        }
    }


    @GetMapping("/refund")
    public String refund() throws Exception {
        Factory.setOptions(config);
        AlipayTradeRefundResponse refundResponse = Factory.Payment.Common().refund("LS123qwe123", "19999");
        return refundResponse.getHttpBody();
    }
}