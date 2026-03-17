package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.PageResult;
import com.rmm.entity.Location;
import com.rmm.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;

    public PageResult<Location> list(Integer current, Integer size, String keyword, Integer status) {
        Page<Location> page = new Page<>(current, size);

        LambdaQueryWrapper<Location> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Location::getStatus, status)
               .and(StringUtils.hasText(keyword), w -> w
                   .like(Location::getCode, keyword)
                   .or()
                   .like(Location::getName, keyword)
               )
               .orderByDesc(Location::getCreateTime);

        Page<Location> result = locationMapper.selectPage(page, wrapper);

        PageResult<Location> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public List<Location> listAll() {
        return locationMapper.selectList(
            new LambdaQueryWrapper<Location>()
                .eq(Location::getStatus, 1)
                .orderByAsc(Location::getCode)
        );
    }

    public Location getById(Long id) {
        return locationMapper.selectById(id);
    }

    public void create(Location location) {
        location.setStatus(1);
        locationMapper.insert(location);
    }

    public void update(Location location) {
        locationMapper.updateById(location);
    }

    public void delete(Long id) {
        locationMapper.deleteById(id);
    }
}
