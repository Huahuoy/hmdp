package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constants.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Result queryTypeList() {
        //TODO 查redis
        String shopList = (String) redisTemplate.opsForValue().get(RedisConstants.CACHE_TYPE_KEY);
        if(StrUtil.isNotBlank(shopList)){
            return Result.ok(JSONUtil.toList(shopList,ShopType.class));
        }
        //TODO 查数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        redisTemplate.opsForValue().set(RedisConstants.CACHE_TYPE_KEY,
                JSONUtil.toJsonStr(shopTypeList));

        return Result.ok(shopTypeList);
    }

}
