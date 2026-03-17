package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Stock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
}
