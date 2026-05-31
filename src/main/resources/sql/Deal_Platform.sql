-- 校园二手交易系统数据库脚本
-- 数据库名称：Deal_Platform
-- 说明：包含建库、建表、约束、索引和初始测试数据，可直接导入 MySQL 运行。

DROP DATABASE IF EXISTS Deal_Platform;
CREATE DATABASE Deal_Platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE Deal_Platform;

-- 1. 角色表
CREATE TABLE role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(30) NOT NULL UNIQUE COMMENT '角色编码：admin/user',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2. 用户表
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '登录密码，项目中建议存储加密后的密码',
  real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  student_no VARCHAR(30) DEFAULT NULL UNIQUE COMMENT '学号',
  phone VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '邮箱',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
  gender TINYINT DEFAULT 0 COMMENT '性别：0未知，1男，2女',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
  last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 用户地址表
CREATE TABLE user_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  contact_name VARCHAR(50) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  campus_area VARCHAR(100) NOT NULL COMMENT '校区或区域',
  detail_address VARCHAR(255) NOT NULL COMMENT '详细交易地点',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0否，1是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- 4. 商品分类表
CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级分类ID',
  category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_category_name_parent (category_name, parent_id),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 5. 商品表
CREATE TABLE goods (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
  seller_id BIGINT NOT NULL COMMENT '卖家用户ID',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  title VARCHAR(100) NOT NULL COMMENT '商品标题',
  description TEXT NOT NULL COMMENT '商品描述',
  price DECIMAL(10,2) NOT NULL COMMENT '价格',
  original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  condition_level TINYINT NOT NULL DEFAULT 3 COMMENT '成色：1较旧，2一般，3良好，4很新，5全新',
  trade_place VARCHAR(255) DEFAULT NULL COMMENT '交易地点',
  cover_image_url VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态：0待审核，1已上架，2已下架，3已售出，4审核失败',
  audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
  publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  CONSTRAINT fk_goods_seller FOREIGN KEY (seller_id) REFERENCES user(id),
  CONSTRAINT fk_goods_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT ck_goods_price CHECK (price >= 0),
  CONSTRAINT ck_goods_original_price CHECK (original_price IS NULL OR original_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE INDEX idx_goods_status ON goods(status);
CREATE INDEX idx_goods_category ON goods(category_id);
CREATE INDEX idx_goods_seller ON goods(seller_id);
CREATE INDEX idx_goods_title ON goods(title);

-- 6. 商品图片表
CREATE TABLE goods_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  image_url VARCHAR(255) NOT NULL COMMENT '图片路径',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_goods_image_goods FOREIGN KEY (goods_id) REFERENCES goods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- 7. 商品审核记录表
CREATE TABLE goods_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审核记录ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  auditor_id BIGINT NOT NULL COMMENT '审核管理员ID',
  audit_status TINYINT NOT NULL COMMENT '审核结果：1通过，4驳回',
  audit_comment VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
  audit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  CONSTRAINT fk_audit_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
  CONSTRAINT fk_audit_user FOREIGN KEY (auditor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品审核记录表';

-- 8. 收藏表
CREATE TABLE favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_favorite_user_goods (user_id, goods_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_favorite_goods FOREIGN KEY (goods_id) REFERENCES goods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- 9. 浏览记录表
CREATE TABLE browse_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '浏览记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  browse_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  UNIQUE KEY uk_history_user_goods (user_id, goods_id),
  CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_history_goods FOREIGN KEY (goods_id) REFERENCES goods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录表';

-- 10. 订单表
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  buyer_id BIGINT NOT NULL COMMENT '买家ID',
  seller_id BIGINT NOT NULL COMMENT '卖家ID',
  total_amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
  trade_place VARCHAR(255) DEFAULT NULL COMMENT '约定交易地点',
  buyer_remark VARCHAR(255) DEFAULT NULL COMMENT '买家备注',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待确认，1交易中，2已完成，3已取消',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
  cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_order_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
  CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES user(id),
  CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES user(id),
  CONSTRAINT ck_order_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);

-- 11. 订单状态记录表
CREATE TABLE order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态记录ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  old_status TINYINT DEFAULT NULL COMMENT '原状态',
  new_status TINYINT NOT NULL COMMENT '新状态',
  operator_id BIGINT NOT NULL COMMENT '操作人ID',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_order_log_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_log_user FOREIGN KEY (operator_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态记录表';

-- 12. 留言表
CREATE TABLE message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '留言ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  user_id BIGINT NOT NULL COMMENT '留言用户ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父留言ID，用于回复',
  content VARCHAR(500) NOT NULL COMMENT '留言内容',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0隐藏，1正常',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  CONSTRAINT fk_message_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
  CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_message_parent FOREIGN KEY (parent_id) REFERENCES message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品留言表';

-- 13. 评价表
CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
  order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
  goods_id BIGINT NOT NULL COMMENT '商品ID',
  buyer_id BIGINT NOT NULL COMMENT '买家ID',
  seller_id BIGINT NOT NULL COMMENT '卖家ID',
  score TINYINT NOT NULL COMMENT '评分：1-5',
  content VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_review_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
  CONSTRAINT fk_review_buyer FOREIGN KEY (buyer_id) REFERENCES user(id),
  CONSTRAINT fk_review_seller FOREIGN KEY (seller_id) REFERENCES user(id),
  CONSTRAINT ck_review_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易评价表';

-- 14. 举报表
CREATE TABLE report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
  reporter_id BIGINT NOT NULL COMMENT '举报人ID',
  target_type VARCHAR(30) NOT NULL COMMENT '举报对象类型：goods/user/message',
  target_id BIGINT NOT NULL COMMENT '举报对象ID',
  reason VARCHAR(100) NOT NULL COMMENT '举报原因',
  description VARCHAR(500) DEFAULT NULL COMMENT '详细说明',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待处理，1已处理，2已驳回',
  handle_result VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  handler_id BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user(id),
  CONSTRAINT fk_report_handler FOREIGN KEY (handler_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- 15. 系统公告表
CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
  title VARCHAR(100) NOT NULL COMMENT '公告标题',
  content TEXT NOT NULL COMMENT '公告内容',
  publisher_id BIGINT NOT NULL COMMENT '发布人ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0隐藏，1发布',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 16. 站内消息表
CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  receiver_id BIGINT NOT NULL COMMENT '接收人ID',
  title VARCHAR(100) NOT NULL COMMENT '消息标题',
  content VARCHAR(500) NOT NULL COMMENT '消息内容',
  type VARCHAR(30) NOT NULL COMMENT '消息类型：audit/order/message/report/notice',
  related_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- 17. 操作日志表
CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
  username VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
  operation_content VARCHAR(500) NOT NULL COMMENT '操作内容',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 18. 首页轮播图表
CREATE TABLE banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '轮播图ID',
  title VARCHAR(100) NOT NULL COMMENT '标题',
  image_url VARCHAR(255) NOT NULL COMMENT '图片路径',
  link_url VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图表';

-- 19. 模拟支付记录表
CREATE TABLE payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付记录ID',
  order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
  buyer_id BIGINT NOT NULL COMMENT '买家ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_type VARCHAR(30) NOT NULL DEFAULT 'offline' COMMENT '支付方式：offline/mock',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款',
  transaction_no VARCHAR(80) DEFAULT NULL UNIQUE COMMENT '模拟交易流水号',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_payment_buyer FOREIGN KEY (buyer_id) REFERENCES user(id),
  CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟支付记录表';

-- 20. 文件资源表
CREATE TABLE file_resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
  uploader_id BIGINT NOT NULL COMMENT '上传人ID',
  file_name VARCHAR(150) NOT NULL COMMENT '原文件名',
  file_url VARCHAR(255) NOT NULL COMMENT '文件访问路径',
  file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
  file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
  business_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型：avatar/goods/notice/banner',
  business_id BIGINT DEFAULT NULL COMMENT '业务ID',
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES user(id),
  CONSTRAINT ck_file_size CHECK (file_size >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

-- 21. 数据字典类型表
CREATE TABLE dict_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典类型ID',
  dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
  dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

-- 22. 数据字典数据表
CREATE TABLE dict_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
  dict_type_id BIGINT NOT NULL COMMENT '字典类型ID',
  dict_label VARCHAR(100) NOT NULL COMMENT '显示名称',
  dict_value VARCHAR(50) NOT NULL COMMENT '字典值',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_dict_data_value (dict_type_id, dict_value),
  CONSTRAINT fk_dict_data_type FOREIGN KEY (dict_type_id) REFERENCES dict_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典数据表';

-- 初始数据
INSERT INTO role (id, role_code, role_name, description) VALUES
(1, 'admin', '管理员', '系统后台管理员，拥有全部管理权限'),
(2, 'user', '普通用户', '校园用户，可发布、购买、收藏、留言');

INSERT INTO user (id, role_id, username, password, real_name, nickname, student_no, phone, email, status) VALUES
(1, 1, 'kk', '666666', '系统管理员', '管理员', NULL, '13800000000', 'admin@example.com', 1),
(2, 2, 'robin', '123123', 'Robin', 'robin', '20260001', '13800000001', 'robin@example.com', 1);

INSERT INTO category (id, parent_id, category_name, sort_order, status) VALUES
(1, NULL, '教材书籍', 1, 1),
(2, NULL, '电子产品', 2, 1),
(3, NULL, '生活用品', 3, 1),
(4, NULL, '运动器材', 4, 1),
(5, NULL, '服饰鞋包', 5, 1);

INSERT INTO user_address (user_id, contact_name, contact_phone, campus_area, detail_address, is_default) VALUES
(2, 'Robin', '13800000001', '主校区', '图书馆门口', 1);

INSERT INTO goods (id, seller_id, category_id, title, description, price, original_price, condition_level, trade_place, status, cover_image_url) VALUES
(1, 2, 1, 'JavaEE 开发技术教材', '课程教材，笔记较少，适合复习使用。', 25.00, 58.00, 4, '主校区图书馆门口', 1, '/uploads/goods/javaee-book.svg'),
(2, 2, 2, '二手蓝牙耳机', '正常使用，续航良好，外观轻微磨损。', 60.00, 199.00, 3, '东校区二食堂门口', 1, '/uploads/goods/earphone.svg');

INSERT INTO goods_image (goods_id, image_url, sort_order) VALUES
(1, '/uploads/goods/javaee-book.svg', 1),
(2, '/uploads/goods/earphone.svg', 1);

INSERT INTO notice (title, content, publisher_id, status) VALUES
('校园二手交易平台上线通知', '请同学们文明发布商品信息，线下交易注意安全。', 1, 1);

INSERT INTO banner (title, image_url, link_url, sort_order, status) VALUES
('二手教材专区', '/uploads/banner/book-zone.svg', '/goods?categoryId=1', 1, 1),
('电子产品专区', '/uploads/banner/digital-zone.svg', '/goods?categoryId=2', 2, 1);

INSERT INTO dict_type (id, dict_code, dict_name) VALUES
(1, 'goods_status', '商品状态'),
(2, 'order_status', '订单状态'),
(3, 'report_status', '举报状态');

INSERT INTO dict_data (dict_type_id, dict_label, dict_value, sort_order) VALUES
(1, '待审核', '0', 1),
(1, '已上架', '1', 2),
(1, '已下架', '2', 3),
(1, '已售出', '3', 4),
(1, '审核失败', '4', 5),
(2, '待确认', '0', 1),
(2, '交易中', '1', 2),
(2, '已完成', '2', 3),
(2, '已取消', '3', 4),
(3, '待处理', '0', 1),
(3, '已处理', '1', 2),
(3, '已驳回', '2', 3);
