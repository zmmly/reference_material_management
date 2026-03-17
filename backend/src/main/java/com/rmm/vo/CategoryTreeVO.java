package com.rmm.vo;

import lombok.Data;
import java.util.List;

/**
 * 分类树形结构VO
 * 用于前端Element Plus Tree组件
 */
@Data
public class CategoryTreeVO {
    private Long id;
    private String label;  // 对应Category的name字段
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<CategoryTreeVO> children;
}
