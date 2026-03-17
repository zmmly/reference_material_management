package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
