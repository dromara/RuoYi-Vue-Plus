package com.ruoyi.web.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.dto.UserOnlineDTO;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.RedisUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysUserOnline;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户监控
 *
 * @author Lion Li
 */
@Api(value = "在线用户监控", tags = {"在线用户监控管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController {

    @ApiOperation("在线用户列表")
    @SaCheckPermission("monitor:online:list")
    @GetMapping("/list")
    public TableDataInfo<SysUserOnline> list(String ipaddr, String userName) {
        // 获取所有未过期的 token
        List<String> keys = StpUtil.searchTokenValue("", -1, 0);
        List<UserOnlineDTO> userOnlineDTOList = new ArrayList<>();
        for (String key : keys) {
            // 如果已经过期则踢下线
            if (StpUtil.stpLogic.getTokenActivityTimeoutByToken(key) < 0) {
                StpUtil.kickoutByTokenValue(key);
            }
            String onlineKey = key.replace(Constants.LOGIN_TOKEN_KEY, Constants.ONLINE_TOKEN_KEY);
            userOnlineDTOList.add(RedisUtils.getCacheObject(onlineKey));
        }
        if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName)) {
            userOnlineDTOList = userOnlineDTOList.stream().filter(userOnline ->
                StringUtils.equals(ipaddr, userOnline.getIpaddr()) &&
                    StringUtils.equals(userName, userOnline.getUserName())
            ).collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(ipaddr)) {
            userOnlineDTOList = userOnlineDTOList.stream().filter(userOnline ->
                StringUtils.equals(ipaddr, userOnline.getIpaddr()))
                .collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(userName)) {
            userOnlineDTOList = userOnlineDTOList.stream().filter(userOnline ->
                StringUtils.equals(userName, userOnline.getUserName())
            ).collect(Collectors.toList());
        }
        Collections.reverse(userOnlineDTOList);
        userOnlineDTOList.removeAll(Collections.singleton(null));
        List<SysUserOnline> userOnlineList = BeanUtil.copyToList(userOnlineDTOList, SysUserOnline.class);
        return PageUtils.buildDataInfo(userOnlineList);
    }

    /**
     * 强退用户
     */
    @ApiOperation("强退用户")
    @SaCheckPermission("monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult<Void> forceLogout(@PathVariable String tokenId) {
        try {
            StpUtil.kickoutByTokenValue(tokenId);
        } catch (NotLoginException e) {
        }
        return AjaxResult.success();
    }
}
