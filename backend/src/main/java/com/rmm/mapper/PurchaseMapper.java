package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Purchase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseMapper extends BaseMapper<Purchase> {
}
