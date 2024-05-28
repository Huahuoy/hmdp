package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.hmdp.constants.RedisConstants.FOLLOW_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + userId;
        if(isFollow){
            Follow follow = Follow.builder().userId(userId).followUserId(followUserId).build();
            boolean isSuccess = save(follow);
            if(isSuccess){
                redisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else {
            boolean isSuccess = remove(new QueryWrapper<Follow>().
                    eq("user_id", userId).eq("follow_user_id", followUserId));
            if(isSuccess){
                redisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("follow_user_id", followUserId).eq("user_id", userId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key1 = FOLLOW_KEY + userId;
        String key2 = FOLLOW_KEY + id;
        Set<String> common = redisTemplate.opsForSet().intersect(key2, key1);
        List<UserDTO> userDTOS = new ArrayList<>(common.size());
        for (String s : common) {
            User user = userService.getById(Long.valueOf(s));
            userDTOS.add(BeanUtil.copyProperties(user,UserDTO.class));
        }
        return Result.ok(userDTOS);
    }
}
