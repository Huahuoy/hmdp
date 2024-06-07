package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Pocket;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface PocketMapper extends BaseMapper<Pocket> {
    @Update("update tb_pocket set `check` = `check` - #{payValue} where user_id = #{userId}")
    void pay(Long userId, Long payValue);
    @Select("select * from tb_pocket where user_id = #{userId}")
    Pocket getOnePocket(Long userId);
}
