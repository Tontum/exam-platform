-- 添加管理员权限范围字段
ALTER TABLE user ADD COLUMN scope VARCHAR(20) DEFAULT NULL COMMENT '管理员权限范围：ALL=全国、PROVINCE=省级' AFTER role;
ALTER TABLE user ADD COLUMN province VARCHAR(50) DEFAULT NULL COMMENT '管理员所属省份（scope=PROVINCE时必填）' AFTER scope;

-- 超级管理员
UPDATE user SET scope = 'ALL' WHERE id = 1;

-- 校长和老师不需要 scope
UPDATE user SET scope = NULL WHERE role IN (2, 3);

-- 省级管理员示例（如需创建省级管理员，取消注释并修改 ID）
-- UPDATE user SET scope = 'PROVINCE', province = '北京市' WHERE id = {admin_id};
