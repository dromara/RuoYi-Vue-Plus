# PMS房型、房间、房间锁定管理功能部署指南

## 部署前准备

### 环境要求
- Java 17+
- MySQL 8.0+
- Node.js 18+
- Redis 6.0+

### 项目结构确认
确保以下文件已正确放置：


#### 后端文件
```
ruoyi-modules/ruoyi-pms/src/main/java/org/dromara/pms/
├── domain/
│   ├── PmsRoom.java
│   ├── PmsRoomLock.java
│   └── PmsRoomType.java
├── domain/bo/
│   ├── PmsRoomBo.java
│   ├── PmsRoomLockBo.java
│   └── PmsRoomTypeBo.java
├── domain/vo/
│   ├── PmsRoomVo.java
│   ├── PmsRoomLockVo.java
│   └── PmsRoomTypeVo.java
├── mapper/
│   ├── PmsRoomMapper.java
│   ├── PmsRoomLockMapper.java
│   └── PmsRoomTypeMapper.java
├── service/
│   ├── IPmsRoomService.java
│   ├── IPmsRoomLockService.java
│   ├── IPmsRoomTypeService.java
│   └── impl/
│       ├── PmsRoomServiceImpl.java
│       ├── PmsRoomLockServiceImpl.java
│       └── PmsRoomTypeServiceImpl.java
└── controller/
    ├── PmsRoomController.java
    ├── PmsRoomLockController.java
    └── PmsRoomTypeController.java
```

#### 前端文件
```
ruoyi-plus-soybean/src/
├── service/api/pms/
│   ├── index.ts
│   ├── room.ts
│   ├── roomLock.ts
│   └── roomType.ts
├── typings/api/
│   └── pms.api.d.ts
└── views/pms/
    ├── room/
    │   ├── index.vue
    │   └── modules/
    │       └── room-operate-drawer.vue
    ├── roomLock/
    │   ├── index.vue
    │   └── modules/
    │       └── room-lock-operate-drawer.vue
    └── roomType/
        ├── index.vue
        └── modules/
            └── room-type-operate-drawer.vue
```

## 数据库部署

### 1. 创建数据库表

执行以下SQL脚本创建表结构：

```sql
-- 房型表
CREATE TABLE `pms_room_types` (
  `room_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '房型ID',
  `type_name` varchar(100) NOT NULL COMMENT '房型名称',
  `type_code` varchar(20) NOT NULL COMMENT '房型代码',
  `description` text COMMENT '房型描述',
  `standard_occupancy` int NOT NULL DEFAULT '1' COMMENT '标准入住人数',
  `max_occupancy` int NOT NULL DEFAULT '2' COMMENT '最大入住人数',
  `room_area` decimal(8,2) DEFAULT NULL COMMENT '房间面积(平方米)',
  `bed_configuration` varchar(200) DEFAULT NULL COMMENT '床型配置',
  `amenities` text COMMENT '房间设施',
  `default_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '默认价格',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '房型状态(active:启用,inactive:禁用,maintenance:维护中)',
  `sort_order` int DEFAULT '0' COMMENT '排序值',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`room_type_id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房型表';

-- 房间表
CREATE TABLE `pms_room_rooms` (
  `room_id` bigint NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `room_type_id` bigint NOT NULL COMMENT '房型ID',
  `room_number` varchar(20) NOT NULL COMMENT '房间号',
  `floor` varchar(10) NOT NULL COMMENT '楼层',
  `room_status` varchar(20) NOT NULL DEFAULT 'available' COMMENT '房间状态(available:可用,occupied:已入住,maintenance:维护中,out_of_order:故障)',
  `cleaning_status` varchar(20) NOT NULL DEFAULT 'clean' COMMENT '清洁状态(clean:已清洁,dirty:待清洁,cleaning:清洁中,inspecting:检查中)',
  `description` varchar(500) DEFAULT NULL COMMENT '房间描述',
  `special_amenities` text COMMENT '特殊设施',
  `status_remarks` varchar(500) DEFAULT NULL COMMENT '状态备注',
  `last_cleaning_time` datetime DEFAULT NULL COMMENT '最后清洁时间',
  `sort_order` int DEFAULT '0' COMMENT '排序值',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `uk_room_number` (`room_number`),
  KEY `idx_room_type_id` (`room_type_id`),
  KEY `idx_floor` (`floor`),
  KEY `idx_room_status` (`room_status`),
  CONSTRAINT `fk_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房间表';

-- 房间锁定表
CREATE TABLE `pms_room_locks` (
  `lock_id` bigint NOT NULL AUTO_INCREMENT COMMENT '锁定ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `lock_type` varchar(20) NOT NULL COMMENT '锁定类型(maintenance:维护,cleaning:清洁,management:管理,malfunction:故障)',
  `lock_status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '锁定状态(active:锁定中,unlocked:已解锁,expired:已过期)',
  `lock_start_time` datetime NOT NULL COMMENT '锁定开始时间',
  `lock_end_time` datetime DEFAULT NULL COMMENT '锁定结束时间',
  `lock_reason` varchar(500) NOT NULL COMMENT '锁定原因',
  `unlock_time` datetime DEFAULT NULL COMMENT '解锁时间',
  `unlock_by` varchar(64) DEFAULT NULL COMMENT '解锁人',
  `unlock_reason` varchar(500) DEFAULT NULL COMMENT '解锁原因',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`lock_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_lock_type` (`lock_type`),
  KEY `idx_lock_status` (`lock_status`),
  KEY `idx_lock_start_time` (`lock_start_time`),
  CONSTRAINT `fk_lock_room` FOREIGN KEY (`room_id`) REFERENCES `pms_room_rooms` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房间锁定表';
```

### 2. 插入菜单配置

```sql
-- 插入PMS主菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('PMS管理', 0, 4, 'pms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'hotel', 103, 'admin', NOW(), 'admin', NOW(), 'PMS物业管理系统');

-- 获取PMS主菜单ID（假设为2000）
SET @pms_menu_id = LAST_INSERT_ID();

-- 插入房型管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('房型管理', @pms_menu_id, 1, 'roomtype', 'pms/roomType/index', NULL, 1, 0, 'C', '0', '0', 'pms:roomType:list', 'bed', 103, 'admin', NOW(), 'admin', NOW(), '房型管理菜单');

SET @room_type_menu_id = LAST_INSERT_ID();

-- 房型管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('房型查询', @room_type_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomType:query', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房型新增', @room_type_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomType:add', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房型修改', @room_type_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomType:edit', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房型删除', @room_type_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomType:remove', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房型导出', @room_type_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomType:export', '#', 103, 'admin', NOW(), 'admin', NOW(), '');

-- 插入房间管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('房间管理', @pms_menu_id, 2, 'room', 'pms/room/index', NULL, 1, 0, 'C', '0', '0', 'pms:room:list', 'door', 103, 'admin', NOW(), 'admin', NOW(), '房间管理菜单');

SET @room_menu_id = LAST_INSERT_ID();

-- 房间管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('房间查询', @room_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:room:query', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房间新增', @room_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:room:add', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房间修改', @room_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:room:edit', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房间删除', @room_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:room:remove', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房间导出', @room_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:room:export', '#', 103, 'admin', NOW(), 'admin', NOW(), '');

-- 插入房间锁定管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('房间锁定管理', @pms_menu_id, 3, 'roomlock', 'pms/roomLock/index', NULL, 1, 0, 'C', '0', '0', 'pms:roomLock:list', 'lock', 103, 'admin', NOW(), 'admin', NOW(), '房间锁定管理菜单');

SET @room_lock_menu_id = LAST_INSERT_ID();

-- 房间锁定管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
('锁定查询', @room_lock_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomLock:query', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('锁定新增', @room_lock_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomLock:add', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('锁定修改', @room_lock_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomLock:edit', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('锁定删除', @room_lock_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomLock:remove', '#', 103, 'admin', NOW(), 'admin', NOW(), ''),
('房间解锁', @room_lock_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'pms:roomLock:unlock', '#', 103, 'admin', NOW(), 'admin', NOW(), '');
```

### 3. 插入测试数据（可选）

```sql
-- 插入测试房型数据
INSERT INTO pms_room_types (type_name, type_code, description, standard_occupancy, max_occupancy, room_area, bed_configuration, amenities, default_price, status, sort_order, create_by, create_time) VALUES
('标准间', 'STD', '标准双人间，配备基础设施', 2, 2, 25.00, '双床', '空调,电视,WiFi,独立卫浴', 299.00, 'active', 1, 'admin', NOW()),
('豪华间', 'DLX', '豪华双人间，设施齐全', 2, 3, 35.00, '大床', '空调,电视,WiFi,独立卫浴,迷你吧', 499.00, 'active', 2, 'admin', NOW()),
('套房', 'STE', '豪华套房，独立客厅', 2, 4, 60.00, '大床+沙发床', '空调,电视,WiFi,独立卫浴,迷你吧,客厅', 899.00, 'active', 3, 'admin', NOW());

-- 插入测试房间数据
INSERT INTO pms_room_rooms (room_type_id, room_number, floor, room_status, cleaning_status, description, create_by, create_time) VALUES
(1, '101', '1', 'available', 'clean', '一楼标准间', 'admin', NOW()),
(1, '102', '1', 'available', 'clean', '一楼标准间', 'admin', NOW()),
(2, '201', '2', 'available', 'clean', '二楼豪华间', 'admin', NOW()),
(2, '202', '2', 'occupied', 'dirty', '二楼豪华间', 'admin', NOW()),
(3, '301', '3', 'available', 'clean', '三楼套房', 'admin', NOW());
```

## 后端部署

### 1. 编译项目

```bash
cd ruoyi-vue-plus
mvn clean compile
```

### 2. 启动应用

```bash
# 开发环境
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 3. 验证后端API

访问Swagger文档验证API：
- http://localhost:8080/doc.html
- 查找PMS相关接口：
  - `/pms/roomType/**` - 房型管理接口
  - `/pms/room/**` - 房间管理接口
  - `/pms/roomLock/**` - 房间锁定管理接口

## 前端部署

### 1. 安装依赖

```bash
cd ruoyi-plus-soybean
pnpm install
```

### 2. 启动开发服务器

```bash
pnpm dev
```

### 3. 构建生产版本

```bash
pnpm build
```

### 4. 验证前端页面

访问以下页面验证功能：
- http://localhost:9527/pms/roomtype - 房型管理
- http://localhost:9527/pms/room - 房间管理
- http://localhost:9527/pms/roomlock - 房间锁定管理

## 功能测试

### 1. 房型管理测试

1. **新增房型**
   - 填写房型名称、代码等信息
   - 验证表单验证规则
   - 确认保存成功

2. **查询房型**
   - 测试搜索功能
   - 验证分页功能
   - 检查数据显示

3. **编辑房型**
   - 修改房型信息
   - 验证更新成功

4. **删除房型**
   - 单个删除
   - 批量删除

5. **导出功能**
   - 测试数据导出

### 2. 房间管理测试

1. **新增房间**
   - 选择房型
   - 填写房间信息
   - 验证房间号格式

2. **状态管理**
   - 测试房间状态切换
   - 测试清洁状态管理
   - 批量清洁状态更新

3. **搜索筛选**
   - 按房间号搜索
   - 按状态筛选

### 3. 房间锁定管理测试

1. **锁定房间**
   - 选择房间
   - 设置锁定类型和时间
   - 填写锁定原因

2. **解锁房间**
   - 测试解锁功能
   - 验证解锁记录

3. **状态查询**
   - 按锁定类型筛选
   - 按锁定状态筛选

## 权限配置

### 1. 角色权限分配

在系统管理 -> 角色管理中为相关角色分配PMS权限：

- **管理员角色**：所有PMS权限
- **前台角色**：房间查询、状态更新权限
- **清洁员角色**：房间清洁状态更新权限
- **维护员角色**：房间锁定、解锁权限

### 2. 用户权限验证

测试不同角色用户的权限控制：
- 菜单显示控制
- 按钮权限控制
- API接口权限控制

## 性能优化

### 1. 数据库优化

```sql
-- 添加索引优化查询性能
CREATE INDEX idx_room_type_status ON pms_room_types(status);
CREATE INDEX idx_room_status_cleaning ON pms_room_rooms(room_status, cleaning_status);
CREATE INDEX idx_lock_status_type ON pms_room_locks(lock_status, lock_type);
```

### 2. 缓存配置

在application.yml中配置Redis缓存：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10分钟
```

## 监控和日志

### 1. 日志配置

确保logback-spring.xml中包含PMS模块日志：

```xml
<logger name="org.dromara.pms" level="INFO" additivity="false">
    <appender-ref ref="ASYNC_FILE"/>
</logger>
```

### 2. 监控指标

关注以下监控指标：
- API响应时间
- 数据库连接池状态
- 内存使用情况
- 错误日志统计

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库配置
   - 验证数据库服务状态

2. **菜单不显示**
   - 检查菜单配置SQL是否执行
   - 验证用户权限分配

3. **前端页面404**
   - 检查路由配置
   - 验证组件文件路径

4. **API调用失败**
   - 检查后端服务状态
   - 验证API权限配置

### 日志查看

```bash
# 查看应用日志
tail -f logs/sys-info.log

# 查看错误日志
tail -f logs/sys-error.log
```

## 备份和恢复

### 数据备份

```bash
# 备份数据库
mysqldump -u root -p ruoyi_vue_plus > pms_backup.sql

# 备份特定表
mysqldump -u root -p ruoyi_vue_plus pms_room_types pms_room_rooms pms_room_locks > pms_tables_backup.sql
```

### 数据恢复

```bash
# 恢复数据库
mysql -u root -p ruoyi_vue_plus < pms_backup.sql
```

## 版本升级

### 升级步骤

1. 备份当前数据
2. 停止应用服务
3. 更新代码文件
4. 执行数据库升级脚本
5. 重启应用服务
6. 验证功能正常

### 回滚方案

1. 停止应用服务
2. 恢复代码到上一版本
3. 恢复数据库备份
4. 重启应用服务

## 联系支持

如遇到部署问题，请提供以下信息：
- 错误日志
- 环境配置
- 操作步骤
- 预期结果vs实际结果

部署完成后，PMS房型、房间、房间锁定管理功能即可正常使用。 