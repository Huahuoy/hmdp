package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.hmdp.constants.RedisConstants.LOCK_SHOP_KEY;

@Component
@Slf4j
public class CacheClient {

    @Autowired
    private final StringRedisTemplate redisTemplate;


    private final static ExecutorService executor = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private boolean tryLock(String key){
        Boolean b = redisTemplate.opsForValue().setIfAbsent(key, "lock",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }

    private void unLock(String key){
        redisTemplate.delete(key);
    }

    public void setWithLogicExpiration(String key,Object value,Long time,TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R querywithPassThrough(String keyPrefix, ID id, Class<R> type,
                                         Function<ID,R> function,Long time, TimeUnit unit){
        String key = keyPrefix + id;


        String value = redisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(value)){
            log.debug("从redis中拿到数据");
            return JSONUtil.toBean(value,type);
        }
        if(value != null){ //空字符也是数据 如果走到这一步 说明 从redis中拿到的就是之前保存过的null字符串
            return null;
        }
        R r = function.apply(id);

        if(r == null){
            log.debug("缓存穿透...");
            redisTemplate.opsForValue().set(key,"",
                    time,unit);
            return null;
        }
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),
                time,unit);

        return r;
//        String key = keyPrefix + id;
//
//        String value = redisTemplate.opsForValue().get(key);
//
//        if(StrUtil.isNotBlank(value)){
//            log.debug("从redis中拿到数据");
//            return JSONUtil.toBean(value,type);
//        }
//        if(value != null){ //空字符也是数据 如果走到这一步 说明 从redis中拿到的就是之前保存过的null字符串
//            return null;
//        }
//        R r = function.apply(id);
//
//        if(r == null){
//            log.debug("缓存穿透...");
//            redisTemplate.opsForValue().set(key,"",
//                    time,unit);
//            return null;
//        }
//        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),
//                time,unit);
//
//        return r;

    }

    public <R,ID> R querywithMutex(String keyPrefix, ID id, Class<R> type,
                                         Function<ID,R> function,Long time, TimeUnit unit) {

        String key = keyPrefix + id;

        String value = redisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(value)){
            return JSONUtil.toBean(value,type);
        }

        if(value != null){ //空字符也是数据 如果走到这一步 说明 从redis中拿到的就是之前保存过的null字符串
            return null;
        }
        R r = null;
        String lockKey = LOCK_SHOP_KEY + id;


        try {
            boolean isLock = tryLock(lockKey);
            if(!isLock){
                Thread.sleep(50);
                return querywithMutex(keyPrefix,id,type,function,time,unit);
            }
            r = function.apply(id);
            if(r == null){
                log.debug("缓存穿透...");
                redisTemplate.opsForValue().set(key,"",
                        RedisConstants.CACHE_NULL_TTL,unit);
                return null;
            }
            redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),
                    time,unit);
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            unLock(lockKey);
        }
        return r;
    }
    public <R,ID> R querywithLogicExpiration(String keyPrefix, ID id, Class<R> type,
                                   Function<ID,R> function,Long time, TimeUnit unit) {

        String key = keyPrefix + id;

        String value = redisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(value)){
            return null;
        }
        RedisData redisData = JSONUtil.toBean(value, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return r;
        }
        String lockkey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockkey);
        if(isLock){
            try {
                executor.submit(() -> {
                    R apply = function.apply(id);
                    setWithLogicExpiration(key,apply,time,unit);
                });
            }catch (Exception e){
                throw new RuntimeException();
            }finally {
                unLock(lockkey);
            }
        }
        return r;
    }
}
