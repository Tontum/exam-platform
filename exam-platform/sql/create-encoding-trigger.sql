SET NAMES utf8mb4;

-- 删除旧的触发器（如果存在）
DROP TRIGGER IF EXISTS trg_set_encoding;

-- 创建触发器：每次连接时设置编码
DELIMITER //
CREATE TRIGGER trg_set_encoding
AFTER CONNECT ON exam_platform.*
FOR EACH STATEMENT
BEGIN
  SET NAMES utf8mb4;
END//
DELIMITER ;

-- 验证触发器
SHOW TRIGGERS;
