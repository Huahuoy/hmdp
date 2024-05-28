package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.constants.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.constants.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IUserService userService;
    @Autowired
    private IFollowService followService;
    @Override
    public Result likeBlog(Long id) {
        Long userid = UserHolder.getUser().getId();

        Double score = redisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, userid.toString());

        if(score == null){
            //TODO 未点赞 则点赞 并把时间戳作为score
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if(isSuccess){
                redisTemplate.opsForZSet().add(BLOG_LIKED_KEY+id,userid.toString(), System.currentTimeMillis());
            }
        }else {
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                redisTemplate.opsForZSet().remove(BLOG_LIKED_KEY+id,userid.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY +  id;
        Set<String> top5 = redisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());

        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids)
                .last("ORDER BY FIELD(id,"+idStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean save = save(blog);
        if(!save){
            return Result.fail("发布笔记失败！请重试...");
        }
        long time = System.currentTimeMillis();
        List<Follow> followUserId = followService.query().eq("follow_user_id", user.getId()).list();

        for (Follow follow : followUserId) {
            String key = FEED_KEY + follow.getUserId();
            redisTemplate.opsForZSet().add(key,blog.getId().toString(),time);
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryfollowed(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.
                opsForZSet().reverseRangeByScoreWithScores(key, 0,max, offset, 3);
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        long minTime = 0;
        int os = 1;
        List<Long> ids = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {

            String id = tuple.getValue();
            ids.add(Long.valueOf(id));

            long time = tuple.getScore().longValue();

            if(time == minTime){
                os += 1;

            }else{
                minTime = time;
                os = 1;
            }
        }
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).
                last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Blog blog : blogs) {
            isBlogLiked(blog);
            User user = userService.getById(blog.getUserId());
            blog.setIcon(user.getIcon());
            blog.setName(user.getNickName());
        }
        ScrollResult sr = ScrollResult.builder()
                .list(blogs).offset(os).minTime(minTime).build();


        return Result.ok(sr);
    }
    @Override
    public void isBlogLiked(Blog blog)  {
        Long userId = null;
        try {
            userId = UserHolder.getUser().getId();
        }catch (Exception e){
            return;
        }
        //2.判断当前用户是否已经点赞过
        String key = BLOG_LIKED_KEY +blog.getId();
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        if(score != null){
            blog.setIsLike(true);
        }
    }
}
