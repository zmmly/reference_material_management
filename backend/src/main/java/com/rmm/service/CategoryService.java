package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.entity.Category;
import com.rmm.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> list() {
        List<Category> all = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder)
        );
        return buildTree(all);
    }

    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    public void create(Category category) {
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    public void update(Category category) {
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    private List<Category> buildTree(List<Category> categories) {
        Map<Long, Category> map = categories.stream()
            .collect(Collectors.toMap(Category::getId, c -> c));

        List<Category> roots = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                roots.add(category);
            } else {
                Category parent = map.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }
        return roots;
    }
}
