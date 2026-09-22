package org.dromara.workflow.de.cn.service;

import java.util.List;
import java.util.Map;

/**
 * CN 域服务接口 —— P1 档 A：CN-03 共 1 项。
 *
 * @author AIP
 */
public interface IDeCnService {

    /** CN-03 普通员工通过应用 / 动作卡选择连接器（业务表单替代工具 ID） */
    List<Map<String, Object>> selectors(Long userId);
}