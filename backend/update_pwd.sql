USE ecommerce;
UPDATE user SET password = '$2b$10$uEvaM9WcvUbqu/Z9pv3yLu1RNLhUgm9FAtbPEMf03Hu0VRY5Jz58q' WHERE username = 'admin';
UPDATE user SET password = '$2b$10$uEvaM9WcvUbqu/Z9pv3yLu1RNLhUgm9FAtbPEMf03Hu0VRY5Jz58q' WHERE username = 'sales';
UPDATE user SET password = '$2b$10$uEvaM9WcvUbqu/Z9pv3yLu1RNLhUgm9FAtbPEMf03Hu0VRY5Jz58q' WHERE username = 'testuser';
SELECT id, username, password FROM user;
