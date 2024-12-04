package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/*
    实现全局唯一id
 */
@Component
public class RedisIdWorker {

    private StringRedisTemplate redisTemplatel;
    private static final int COUNT_BITS = 32;

    @Autowired
    private ThreadPoolExecutor idWorkExecutor;

    public RedisIdWorker(StringRedisTemplate redisTemplatel) {
        this.redisTemplatel = redisTemplatel;
    }

    public long nextId(String keyPrefix){
        Future<Long> result = idWorkExecutor.submit(() -> {
            LocalDateTime now = LocalDateTime.now();
            long nowStamp = now.toEpochSecond(ZoneOffset.UTC);

            String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
            long orderId = redisTemplatel.opsForValue().increment("incre:" + keyPrefix + date);
            return nowStamp << COUNT_BITS | orderId;
        });
        try {
            return result.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
