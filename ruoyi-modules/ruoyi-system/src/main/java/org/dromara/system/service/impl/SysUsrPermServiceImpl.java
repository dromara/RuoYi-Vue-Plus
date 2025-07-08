package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUsrPerm;
import org.dromara.system.domain.bo.SysUsrPermBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.domain.vo.SysUsrPermVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUsrPermMapper;
import org.dromara.system.service.ISysUsrPermService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SysUsrPermServiceImpl implements ISysUsrPermService {

    private final SysUsrPermMapper baseMapper;
    @Override
    public TableDataInfo<SysUsrPermVo> selectUsrPermList(SysUsrPermBo user, PageQuery pageQuery){

        QueryWrapper<SysUsrPerm> wrapper = Wrappers.query();
        wrapper.like(StringUtils.isNotBlank(user.getUserName()), "user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getUserPerm()), "user_perm", user.getUserPerm()  );

        Page<SysUsrPermVo> page = baseMapper.selectUsrPermList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }
}
