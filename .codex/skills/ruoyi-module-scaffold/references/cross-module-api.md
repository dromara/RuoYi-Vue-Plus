# 跨模块接口（ruoyi-api）

## 架构说明

```
ruoyi-api (契约层)
├── org.dromara.{module}.api/         # 接口
│   ├── {Module}Service.java          # 服务接口（纯 Java 接口，无注解）
│   └── domain/                       # DTO
│       └── {Module}DTO.java
├── org.dromara.{module}.api.event/   # 事件（可选）
│   └── {Module}Event.java
│
ruoyi-modules/ruoyi-{module} (实现层)
└── org.dromara.{module}.service.impl/
    └── {Module}{Business}ServiceImpl.java
        implements I{Module}{Business}Service, {Module}Service  ← 双接口
```

**核心原则**：
- `ruoyi-api` 只放接口和 DTO，不放实现，只依赖 `ruoyi-common-core`。
- 业务模块的 ServiceImpl 同时实现内部接口（`I{Module}Service`）和 API 接口（`{Module}Service`）。
- 其他模块通过注入 API 接口调用，Spring 自动路由到实现类。
- 不是 Dubbo/RPC，是同进程 Spring Bean 注入。

## 包结构

在 `ruoyi-api/src/main/java/org/dromara/` 下新建模块包：

```
ruoyi-api/src/main/java/org/dromara/
├── system/api/          # 已有
├── workflow/api/        # 已有
└── {module}/api/        # 新建
    ├── {Module}Service.java       # 接口
    ├── domain/
    │   └── {Module}DTO.java       # DTO
    └── event/                     # 可选
        └── {Module}Event.java     # 事件
```

## DTO 规则

参考 [UserDTO](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/domain/UserDTO.java)：

```java
package org.dromara.crm.api.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户
 */
@Data
@NoArgsConstructor
public class CrmCustomerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 联系人 */
    private String contactName;

    /** 手机号 */
    private String phoneNumber;
}
```

**约定**：
- `@Data` + `@NoArgsConstructor`。
- `implements Serializable`，加 `@Serial serialVersionUID`。
- 字段用 Java 类型（`Long`、`String`），不用 MyBatis 注解。
- 只暴露其他模块需要的字段，不照搬 Entity 全部字段。

## 接口规则

参考 [UserService](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/UserService.java)：

```java
package org.dromara.crm.api;

import org.dromara.crm.api.domain.CrmCustomerDTO;

import java.util.Collection;
import java.util.List;

/**
 * 通用 客户服务
 */
public interface CrmCustomerService {

    /**
     * 通过客户ID查询客户
     *
     * @param customerId 客户ID
     * @return 客户
     */
    CrmCustomerDTO selectById(Long customerId);

    /**
     * 通过客户ID列表查询客户
     *
     * @param customerIds 客户ID列表
     * @return 客户列表
     */
    List<CrmCustomerDTO> selectListByIds(Collection<Long> customerIds);

    /**
     * 通过客户名称查询客户ID
     *
     * @param customerName 客户名称
     * @return 客户ID
     */
    Long selectIdByName(String customerName);
}
```

**约定**：
- 纯 Java 接口，不加 `@Service` / `@Component` 等注解。
- 方法名以 `select` / `get` 开头（查询类），不暴露写操作（写操作走各自模块 Controller）。
- 返回 DTO，不返回 Entity 或 VO。
- 参数和返回值只用 JDK 类型或 ruoyi-api 内的 DTO。

## 双接口实现

参考 [SysUserServiceImpl](file:///workspace/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java)：

```java
package org.dromara.crm.service.impl;

import org.dromara.crm.api.CrmCustomerService;
import org.dromara.crm.api.domain.CrmCustomerDTO;
import org.dromara.crm.domain.CrmCustomer;
import org.dromara.crm.domain.vo.CrmCustomerVo;
import org.dromara.crm.mapper.CrmCustomerMapper;
import org.dromara.crm.service.ICrmCustomerService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 客户信息 Service 实现
 */
@RequiredArgsConstructor
@Service
public class CrmCustomerServiceImpl implements ICrmCustomerService, CrmCustomerService {

    private final CrmCustomerMapper customerMapper;

    // ========== ICrmCustomerService（内部接口）方法 ==========

    // queryById / queryPageList / queryList / insertByBo / updateByBo / deleteWithValidByIds
    // ...

    // ========== CrmCustomerService（跨模块 API 接口）方法 ==========

    @Override
    public CrmCustomerDTO selectById(Long customerId) {
        CrmCustomerVo vo = customerMapper.selectVoById(customerId);
        return MapstructUtils.convert(vo, CrmCustomerDTO.class);
    }

    @Override
    public List<CrmCustomerDTO> selectListByIds(Collection<Long> customerIds) {
        List<CrmCustomerVo> list = customerMapper.selectVoByIds(customerIds);
        return MapstructUtils.convert(list, CrmCustomerDTO.class);
    }

    @Override
    public Long selectIdByName(String customerName) {
        CrmCustomer customer = customerMapper.selectOne(
            CrmCustomer::getCustomerName, customerName
        );
        return customer != null ? customer.getCustomerId() : null;
    }
}
```

**关键点**：
- `implements ICrmCustomerService, CrmCustomerService` — 同时实现两个接口。
- API 接口方法内部复用 Mapper 查询，VO → DTO 用 `MapstructUtils.convert`。
- API 接口方法不加 `@DataPermission`（跨模块调用通常绕过数据权限）。
- 如果需要绕过数据权限，用 `DataPermissionHelper.ignore(() -> ...)`。

## 事件（可选）

如果新模块需要发布事件供其他模块监听，参考 [workflow/api/event/](file:///workspace/ruoyi-api/src/main/java/org/dromara/workflow/api/event/)：

```java
// ruoyi-api 中定义事件
package org.dromara.crm.api.event;

/**
 * 客户状态变更事件
 */
public record CrmCustomerStatusEvent(Long customerId, String oldStatus, String newStatus) {
}
```

```java
// 其他模块监听
@ConditionalOnClass(CrmCustomerStatusEvent.class)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onCustomerStatusChange(CrmCustomerStatusEvent event) {
    // 处理客户状态变更
}
```

**约定**：
- 事件用 Java 21 `record`。
- 事件类放 `ruoyi-api/{module}/api/event/`，监听者放各自模块。
- 监听者加 `@ConditionalOnClass` 避免模块未加载时启动报错。

## 其他模块调用

```java
// 在 workflow 或其他模块中注入
@RequiredArgsConstructor
@Service
public class SomeServiceImpl implements ISomeService {

    private final CrmCustomerService crmCustomerService;  // 注入 API 接口

    public void doSomething(Long customerId) {
        CrmCustomerDTO customer = crmCustomerService.selectById(customerId);
        // ...
    }
}
```

Spring 自动注入 `CrmCustomerServiceImpl`（因为它 `implements CrmCustomerService`）。

**注意**：调用方模块的 pom.xml 必须依赖 `ruoyi-api`（几乎所有模块都已依赖）。
