package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/*
    实现全局唯一id
 */
@Component
public class RedisIdWorker {
    private long BEGIN_TIMESTAMP = 946684800L;
    private StringRedisTemplate redisTemplatel;

    public RedisIdWorker(StringRedisTemplate redisTemplatel) {
        this.redisTemplatel = redisTemplatel;
    }

    public long nextId(String keyPrefix){
        LocalDateTime now = LocalDateTime.now();
        long nowStamp = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowStamp - BEGIN_TIMESTAMP;
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long orderId = redisTemplatel.opsForValue().increment("incre:" + keyPrefix + date);
        return timeStamp | orderId;
    }

}
