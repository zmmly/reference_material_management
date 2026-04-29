-- 采购申请表新增指定审批人字段
ALTER TABLE purchase ADD COLUMN designated_approver_id bigint DEFAULT NULL COMMENT '指定审批人ID';
