package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiSkillFile;

import java.util.Collection;
import java.util.List;

/**
 * sai_skill_file 服务接口（Map 存根 → typed，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
public interface ISaiSkillFileService extends IService<SaiSkillFile> {

    Long add(SaiSkillFile bo);

    Boolean edit(SaiSkillFile bo);

    Boolean remove(Collection<Long> ids);

    SaiSkillFile getInfo(Long id);

    List<SaiSkillFile> list(SaiSkillFile bo);

    PageResult<SaiSkillFile> page(SaiSkillFile bo);

    List<SaiSkillFile> listByFk(Long fkId);
}
