package com.wudgaby.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wudgaby.stars.domain.StarsTag;
import com.wudgaby.stars.domain.StarsUserRepoTag;
import com.wudgaby.stars.domain.bo.CreateStarsTagBo;
import com.wudgaby.stars.domain.bo.UpdateStarsTagBo;
import com.wudgaby.stars.domain.vo.StarsTagVo;
import com.wudgaby.stars.mapper.StarsTagMapper;
import com.wudgaby.stars.mapper.StarsUserRepoTagMapper;
import com.wudgaby.stars.service.IStarsTagService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户标签服务实现
 */
@RequiredArgsConstructor
@Service
public class StarsTagServiceImpl implements IStarsTagService {

    private final StarsTagMapper tagMapper;
    private final StarsUserRepoTagMapper userRepoTagMapper;

    @Override
    public List<StarsTagVo> listByUser(Long userId) {
        return tagMapper.selectVoList(new LambdaQueryWrapper<StarsTag>()
            .eq(StarsTag::getUserId, userId)
            .orderByAsc(StarsTag::getName));
    }

    @Override
    public StarsTagVo create(Long userId, CreateStarsTagBo request) {
        String name = normalizeName(request.name());
        assertNameAvailable(userId, name, null);

        StarsTag tag = new StarsTag();
        tag.setId(IdGeneratorUtil.nextLongId());
        tag.setUserId(userId);
        tag.setName(name);
        tag.setColor(StringUtils.trim(request.color()));
        tag.setCreateTime(LocalDateTime.now());

        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("标签名称已存在");
        }
        return toVo(tag);
    }

    @Override
    public StarsTagVo update(Long userId, UpdateStarsTagBo request) {
        StarsTag existing = requireOwnedTag(userId, request.id());
        String name = normalizeName(request.name());
        assertNameAvailable(userId, name, existing.getId());

        StarsTag patch = new StarsTag();
        patch.setId(existing.getId());
        patch.setName(name);
        patch.setColor(StringUtils.trim(request.color()));

        try {
            tagMapper.updateById(patch);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("标签名称已存在");
        }

        existing.setName(name);
        existing.setColor(patch.getColor());
        return toVo(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long tagId) {
        requireOwnedTag(userId, tagId);
        userRepoTagMapper.delete(new LambdaQueryWrapper<StarsUserRepoTag>()
            .eq(StarsUserRepoTag::getTagId, tagId));
        tagMapper.deleteById(tagId);
    }

    private StarsTag requireOwnedTag(Long userId, Long tagId) {
        StarsTag tag = tagMapper.selectById(tagId);
        if (tag == null || !userId.equals(tag.getUserId())) {
            throw new ServiceException("标签不存在");
        }
        return tag;
    }

    private void assertNameAvailable(Long userId, String name, Long excludeId) {
        LambdaQueryWrapper<StarsTag> wrapper = new LambdaQueryWrapper<StarsTag>()
            .eq(StarsTag::getUserId, userId)
            .eq(StarsTag::getName, name);
        if (excludeId != null) {
            wrapper.ne(StarsTag::getId, excludeId);
        }
        if (tagMapper.exists(wrapper)) {
            throw new ServiceException("标签名称已存在");
        }
    }

    private static String normalizeName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("标签名称不能为空");
        }
        return name.trim();
    }

    private static StarsTagVo toVo(StarsTag tag) {
        StarsTagVo vo = new StarsTagVo();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setColor(tag.getColor());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }

}
