package com.rmm.vo;

import lombok.Data;
import java.util.List;

@Data
public class TreeNodeVO {
    private Long id;
    private String label;
    private List<TreeNodeVO> children;
}
