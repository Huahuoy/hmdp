package com.hmdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "alipay.easy")
public class AliPayConfigInfo {
 
    /**
     * 请求协议
     */
    private String protocol;
    /**
     * 请求网关
     */
    private String gatewayHost;
    /**
     * 签名类型
     */
    private String signType;
    /**
     * 应用ID（来自支付宝申请）
     */
    private String appId;
    /**
     * 应用秘钥
     */
    private String merchantPrivateKey;
    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;
    /**
     * 支付结果异步通知的地址
     */
    private String notifyUrl;
    /**
     * 设施AES秘钥
     */
    private String encryptKey;
}