package org.dromara.common.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务操作类型
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    /**
     * 其它操作
     */
    OTHER(0, "其它操作"),

    /**
     * 查询
     */
    QUERY(1, "查询"),

    /**
     * 新增
     */
    INSERT(2, "新增"),

    /**
     * 修改
     */
    UPDATE(3, "修改"),

    /**
     * 删除
     */
    DELETE(4, "删除"),

    /**
     * 导入
     */
    IMPORT(5, "导入"),

    /**
     * 导出
     */
    EXPORT(6, "导出"),

    /**
     * 授权/赋权
     */
    GRANT(7, "授权"),

    /**
     * 清空数据
     */
    CLEAN(8, "清空数据"),

    /**
     * 强制退出（用户登出）
     */
    FORCE_LOGOUT(9, "强制退出"),

    /**
     * 状态修改（启用/禁用/冻结/解冻）
     */
    CHANGE_STATUS(10, "状态修改"),

    /**
     * 重置密码
     */
    RESET_PASSWORD(11, "重置密码"),

    /**
     * 批量删除
     */
    BATCH_DELETE(12, "批量删除"),

    /**
     * 批量导入
     */
    BATCH_IMPORT(13, "批量导入"),

    /**
     * 批量导出
     */
    BATCH_EXPORT(14, "批量导出"),

    /**
     * 批量操作（通用）
     */
    BATCH_OPERATE(15, "批量操作"),

    /**
     * 审核操作（通过/驳回）
     */
    AUDIT(16, "审核"),

    /**
     * 发布/上线
     */
    PUBLISH(17, "发布"),

    /**
     * 撤回/下线
     */
    REVOKE(18, "撤回"),

    /**
     * 同步数据
     */
    SYNC(19, "同步数据"),

    /**
     * 生成数据（生成编码、生成报表等）
     */
    GENERATE(20, "生成数据"),

    /**
     * 上传文件
     */
    UPLOAD(21, "上传文件"),

    /**
     * 下载文件
     */
    DOWNLOAD(22, "下载文件"),

    /**
     * 定时任务执行
     */
    TASK_EXECUTE(23, "定时任务执行"),

    /**
     * 接口调用（第三方/开放API）
     */
    API_CALL(24, "接口调用"),

    /**
     * 登录
     */
    LOGIN(25, "登录"),

    /**
     * 登出
     */
    LOGOUT(26, "登出"),

    /**
     * 注册
     */
    REGISTER(27, "注册"),

    /**
     * 作废/取消
     */
    CANCEL(28, "作废/取消"),

    /**
     * 归档
     */
    ARCHIVE(29, "归档"),

    /**
     * 配置修改
     */
    CONFIG_UPDATE(30, "配置修改"),

    /**
     * 打印单据/报表
     */
    PRINT(31, "打印"),

    /**
     * 复制数据/克隆
     */
    COPY(32, "复制"),

    /**
     * 生成代码
     */
    GENCODE(33, "生成代码");

    /**
     * 业务类型编码
     */
    private final Integer code;

    /**
     * 业务类型描述
     */
    private final String desc;

}
