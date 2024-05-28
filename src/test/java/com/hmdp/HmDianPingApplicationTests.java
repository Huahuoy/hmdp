package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.constants.RedisConstants;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ShopServiceImpl shopService;

    @Test
    public void setwithlogicexp(){
        Shop shop = shopService.getById(1);
        cacheClient.setWithLogicExpiration(RedisConstants.CACHE_SHOP_KEY+1, JSONUtil.toJsonStr(shop),30L, TimeUnit.SECONDS);

    }

}
