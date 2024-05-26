package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.hmdp.utils.RedisConstants;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result getByIdWithCache(Long id) {
        //TODO 缓存穿透
//        Shop shop = cacheClient.querywithPassThrough(CACHE_SHOP_KEY, id, Shop.class,
//                this::getById, CACHE_NULL_TTL, TimeUnit.SECONDS);

        //TODO 互斥锁解决缓存击穿
        Shop shop = cacheClient.querywithMutex(CACHE_SHOP_KEY, id, Shop.class,
                this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        //TODO 逻辑过期解决缓存击穿
//        Shop shop = cacheClient.querywithLogicExpiration(CACHE_SHOP_KEY, id, Shop.class,
//                this::getById, CACHE_NULL_TTL, TimeUnit.SECONDS);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }
}
