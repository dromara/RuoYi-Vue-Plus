package org.dromara.system.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysUsrPermBo;
import org.dromara.system.domain.vo.SysUsrPermVo;

import java.util.List;

public interface ISysUsrPermService {

    TableDataInfo<SysUsrPermVo> selectUsrPermList(SysUsrPermBo user, PageQuery pageQuery);
}
