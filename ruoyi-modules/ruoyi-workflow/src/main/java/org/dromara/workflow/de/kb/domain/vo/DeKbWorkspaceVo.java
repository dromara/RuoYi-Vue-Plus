package org.dromara.workflow.de.kb.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * KB-01 知识详情工作区 VO。
 *
 * @author AIP
 */
@Data
public class DeKbWorkspaceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_rag_document.id */
    private Long documentId;

    /** 标题 */
    private String title;

    /** 范围（我维护 / 共享给我 / 可使用） */
    private List<String> scopes;

    /** 内容片段列表 */
    private List<String> snippets;
}