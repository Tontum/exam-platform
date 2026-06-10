SET NAMES utf8mb4;
UPDATE user SET real_name = '王校长', school = '洛阳一高' WHERE id = 13;
UPDATE user SET real_name = '张校长', school = '开封一中' WHERE id = 14;
SELECT id, real_name, school FROM user;
