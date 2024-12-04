package com.hmdp.config;

import com.alipay.easysdk.kernel.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayConfig {
 
    @Bean
    public Config config(AliPayConfigInfo configInfo){
        Config config = new Config();
        config.protocol = configInfo.getProtocol();
        config.gatewayHost = configInfo.getGatewayHost();
        config.signType = configInfo.getSignType();
        config.appId = configInfo.getAppId();
        config.merchantPrivateKey = configInfo.getMerchantPrivateKey();
        config.alipayPublicKey = configInfo.getAlipayPublicKey();
        config.notifyUrl = configInfo.getNotifyUrl();
        config.encryptKey = "";
        return config;
    }
}