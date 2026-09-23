package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigBo;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyConfigVo;

import java.util.List;

/**
 * SY-02 系统配置服务接口（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeSyConfigService {

    IPage<DeSyConfigVo> page(DeSyConfigQueryBo query, Long pageNum, Long pageSize);

    List<DeSyConfigVo> list(DeSyConfigQueryBo query);

    DeSyConfigVo getInfo(Long configId);

    String getConfigByKey(String configKey);

    Long add(DeSyConfigBo bo);

    int edit(DeSyConfigBo bo);

    int remove(Long[] configIds);

    int refreshCache();
}