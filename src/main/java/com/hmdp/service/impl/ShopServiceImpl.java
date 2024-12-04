package com.hmdp.service.impl;

import cn.hutool.bloomfilter.BloomFilterUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.config.RedisConfig;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.constants.RedisConstants.*;


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
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisConfig redisConfig;
    private Integer limitHistory = 500;

    @Autowired
    private RBloomFilterUtils<String> bloomFilterUtil;

    @PostConstruct
    public void initAfterBean(){
        List<Shop> shopList = this.queryShopInit();
        RBloomFilter<String> bloomFilter = bloomFilterUtil.create("shop", 1000L, 0.03);
        for (Shop shop : shopList) {
            bloomFilter.add(String.valueOf(shop.getId()));
            String key = CACHE_SHOP_KEY + shop.getId();
            redisTemplate.opsForValue().set(key,
                    JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
    }

    @Override
    public Result getByIdWithCache(Long id) {
        RBloomFilter<String> bloomFilter = redisConfig.redissonClient().getBloomFilter("shop");
        boolean isContain = bloomFilter.contains(String.valueOf(id));
        if(!isContain) {
            return Result.fail("店铺不存在");
        }
        Shop shop = cacheClient.querywithPassThrough(CACHE_SHOP_KEY, id, Shop.class,
                this::getById, CACHE_NULL_TTL, TimeUnit.SECONDS);

        if(shop == null){
            return Result.fail("店铺不存在");
        }

        UserDTO user = UserHolder.getUser();
        if(user == null){
            return Result.ok(shop);
        }
        //按时间的历史浏览功能 key为前缀+用户id value为商铺id  socre 为时间戳
        String key = HISTORY_KEY + user.getId();
        Double score = redisTemplate.opsForZSet().score(key, String.valueOf(id));
        if(score == null){
            redisTemplate.opsForZSet().add(key, String.valueOf(id),System.currentTimeMillis());
        }else {
            redisTemplate.opsForZSet().remove(key,String.valueOf(id));
        }
        Long currentHistory = redisTemplate.opsForZSet().zCard(key);
        if(currentHistory > limitHistory){
            redisTemplate.opsForZSet().removeRange(key,0,currentHistory-limitHistory-1);
        }
        return Result.ok(shop);
    }

//        //TODO 缓存穿透
//        Shop shop = cacheClient.querywithPassThrough(CACHE_SHOP_KEY, id, Shop.class,
//                this::getById, CACHE_NULL_TTL, TimeUnit.SECONDS);
//
//        //TODO 互斥锁解决缓存击穿
////        Shop shop = cacheClient.querywithMutex(CACHE_SHOP_KEY, id, Shop.class,
////                this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
//        //TODO 逻辑过期解决缓存击穿
////        Shop shop = cacheClient.querywithLogicExpiration(CACHE_SHOP_KEY, id, Shop.class,
////                this::getById, CACHE_NULL_TTL, TimeUnit.SECONDS);
//        if(shop == null){
//            return Result.fail("店铺不存在");
//        }
//        UserDTO user = UserHolder.getUser();
//        if(user == null){
//            return Result.ok(shop);
//        }
//        //按时间的历史浏览功能 key为前缀+用户id value为商铺id  socre 为时间戳
//        String key = HISTORY_KEY + user.getId();
//        Double score = redisTemplate.opsForZSet().score(key, String.valueOf(id));
//        if(score == null){
//            redisTemplate.opsForZSet().add(key, String.valueOf(id),System.currentTimeMillis());
//        }else {
//            redisTemplate.opsForZSet().remove(key,String.valueOf(id));
//        }
//        Long currentHistory = redisTemplate.opsForZSet().zCard(key);
//        if(currentHistory > limitHistory){
//            redisTemplate.opsForZSet().removeRange(key,0,currentHistory-limitHistory-1);
//        }
//        return Result.ok(shop);
//    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {

        //1.判断是否需要根据坐标查询
        if(x==null || y==null){
            //不需要查询坐标，按数据库查
            Page<Shop> page = query()
                    .eq("type_id",typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //2.计算分页参数
        int from = (current - 1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current*SystemConstants.DEFAULT_PAGE_SIZE;
        //3.查询redis，按照距离排序、分页。结果：shopId,distance
        String key = SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        //4.解析出id
        if(results==null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        //4.1.截取from-end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String,Distance> distanceMap = new HashMap<>(list.size());
        if(list.size()<=from){
            return Result.ok(Collections.emptyList());
        }
        list.stream().skip(from).forEach(result->{ //跳过可能把所有数据跳过了
            //4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        //5.根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD ( id," + idStr + ")").list();
        for(Shop shop : shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        //6、返回
        return Result.ok(shops);

    }

    @Override
    public List<Shop> queryShopInit() {
        return query().list();
    }

}
