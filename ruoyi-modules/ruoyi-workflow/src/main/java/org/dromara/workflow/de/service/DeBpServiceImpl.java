package org.dromara.workflow.de.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 03 号 §一 10 业务环节（E0—E9）。
 *
 * <p><b>状态：owner-pending（原 §六 mock 占位已改为显式异常）</b>。
 *
 * <p>改为显式异常的依据：
 * <ul>
 *   <li>入参为无类型 {@code Object}，docs 中查不到任何 payload 字段契约
 *       （{@code docs/归档/2026-09-18-P0重启/03号业务环节实施清单-2026-09-18.md}
 *       §六 原仅为 mock，未定义入参字段），无法在不编造字段名的前提下读参。</li>
 *   <li>各环节真实动作已由底座既有端点承担，本类是重复入口，且 E6/E7/E8/E9 的编号语义
 *       与权威设计 {@code docs/设计包/03-业务流程与状态机.md} §1.2 相抵，须 owner 先对齐。</li>
 * </ul>
 *
 * <p>每个方法抛出指向真实底座端点的 {@link ServiceException}，不再返回伪成功 Map。
 *
 * @author AIP
 */
@Slf4j
@Service
public class DeBpServiceImpl implements IDeBpService {

    /** 统一抛出 owner-pending 异常；{@code target} 指向真实底座端点（不臆造 payload 字段）。 */
    private static Map<String, Object> pending(String phase, String target) {
        throw new ServiceException(
            "规则未定,待owner: 环节 " + phase + " 缺 payload 字段契约且编号语义待对齐，真实动作见 " + target);
    }

    @Override
    public Map<String, Object> e0DraftCheck(Object payload) {
        return pending("E0", "PUT /workflow/definition/publish/{id}");
    }

    @Override
    public Map<String, Object> e1Start(Object payload) {
        return pending("E1", "POST /workflow/task/startWorkFlow");
    }

    @Override
    public Map<String, Object> e2BytecodeGen(Object payload) {
        return pending("E2", "引擎 insService.start 内部建首任务（本仓源码不可见）");
    }

    @Override
    public Map<String, Object> e3AssignUser(Object payload) {
        return pending("E3", "POST /de/binding/assignee");
    }

    @Override
    public Map<String, Object> e4CompleteTask(Object payload) {
        return pending("E4", "POST /workflow/task/completeTask");
    }

    @Override
    public Map<String, Object> e5BackProcess(Object payload) {
        return pending("E5", "POST /workflow/task/backProcess");
    }

    @Override
    public Map<String, Object> e6AbnormalFix(Object payload) {
        return pending("E6", "异常修复落点与判定口径（counter+path）待 owner 裁");
    }

    @Override
    public Map<String, Object> e7Archive(Object payload) {
        return pending("E7", "监听器写 flow_instance.flow_status=finish（D-3 断点）");
    }

    @Override
    public Map<String, Object> e8RevokeOrInvalid(Object payload) {
        return pending("E8", "PUT /workflow/instance/cancelProcessApply 或 POST /workflow/instance/invalid");
    }

    @Override
    public Map<String, Object> e9Transfer(Object payload) {
        return pending("E9", "委托/转办底座接口（TaskOperationEnum）");
    }
}
