package com.hmdp.utils;


import com.hmdp.config.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class RBloomFilterUtils<T> {

    @Resource
    private RedissonClient redissonClient;


    public RBloomFilter<T> create(String bloomName, long expectedInsertions, double falseProbability){
        RBloomFilter<T> bloomFilter = redissonClient.getBloomFilter(bloomName);
        bloomFilter.tryInit(expectedInsertions,falseProbability);
        return bloomFilter;
    }

}
