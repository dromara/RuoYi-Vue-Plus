package org.dromara.workflow.api.domain;

/**
 * 抄送
 *
 * @param userId   抄送用户 ID
 * @param nickName 抄送用户昵称
 * @author may
 */
public record FlowCopyDTO(
    Long userId,
    String nickName
) {
}
