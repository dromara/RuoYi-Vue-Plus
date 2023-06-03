package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 参考官方提供的响应数据
 *
 * @author may
 */
@Data
public class AccountVo {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String fullName;
    private List<String> groups;
    private List<String> privileges;
}
