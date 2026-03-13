package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysLoginInfo;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysLoginInfoVo;
import org.dromara.system.mapper.SysLoginInfoMapper;
import org.dromara.system.service.ISysClientService;
import org.dromara.system.service.ISysLoginInfoService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginInfoServiceImpl implements ISysLoginInfoService {

    private final SysLoginInfoMapper baseMapper;

    private final ISysClientService clientService;

    /**
     * 记录登录信息
     *
     * @param loginInfoEvent 登录事件
     */
    @Async
    @EventListener
    public void recordLoginInfo(LoginInfoEvent loginInfoEvent) {
        HttpServletRequest request = loginInfoEvent.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientId = request.getHeader(LoginHelper.CLIENT_KEY);
        SysClientVo client = null;
        if (StringUtils.isNotBlank(clientId)) {
            client = clientService.queryByClientId(clientId);
        }

        String address = AddressUtils.getRealAddressByIP(ip);
        StringBuilder s = new StringBuilder();
        s.append(getBlock(ip));
        s.append(address);
        s.append(getBlock(loginInfoEvent.getUsername()));
        s.append(getBlock(loginInfoEvent.getStatus()));
        s.append(getBlock(loginInfoEvent.getMessage()));
        // 打印信息到日志
        log.info(s.toString(), loginInfoEvent.getArgs());
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        // 封装对象
        SysLoginInfoBo loginInfo = new SysLoginInfoBo();
        loginInfo.setUserName(loginInfoEvent.getUsername());
        if (ObjectUtil.isNotNull(client)) {
            loginInfo.setClientKey(client.getClientKey());
            loginInfo.setDeviceType(client.getDeviceType());
        }
        loginInfo.setIpaddr(ip);
        loginInfo.setLoginLocation(address);
        loginInfo.setBrowser(browser);
        loginInfo.setOs(os);
        loginInfo.setMsg(loginInfoEvent.getMessage());
        // 日志状态
        if (StringUtils.equalsAny(loginInfoEvent.getStatus(), Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
            loginInfo.setStatus(Constants.SUCCESS);
        } else if (Constants.LOGIN_FAIL.equals(loginInfoEvent.getStatus())) {
            loginInfo.setStatus(Constants.FAIL);
        }
        // 插入数据
        insertLoginInfo(loginInfo);
    }

    /**
     * 将日志片段包装为统一的方括号格式。
     *
     * @param msg 日志片段内容
     * @return 包装后的日志片段字符串
     */
    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }

    /**
     * 分页查询登录日志列表
     *
     * @param loginInfo 查询条件
     * @param pageQuery  分页参数
     * @return 登录日志分页列表
     */
    @Override
    public TableDataInfo<SysLoginInfoVo> selectPageLoginInfoList(SysLoginInfoBo loginInfo, PageQuery pageQuery) {
        Map<String, Object> params = loginInfo.getParams();
        LambdaQueryWrapper<SysLoginInfo> lqw = new LambdaQueryWrapper<SysLoginInfo>()
            .like(StringUtils.isNotBlank(loginInfo.getIpaddr()), SysLoginInfo::getIpaddr, loginInfo.getIpaddr())
            .eq(StringUtils.isNotBlank(loginInfo.getStatus()), SysLoginInfo::getStatus, loginInfo.getStatus())
            .like(StringUtils.isNotBlank(loginInfo.getUserName()), SysLoginInfo::getUserName, loginInfo.getUserName())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysLoginInfo::getLoginTime, params.get("beginTime"), params.get("endTime"));
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            lqw.orderByDesc(SysLoginInfo::getInfoId);
        }
        Page<SysLoginInfoVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 新增系统登录日志
     *
     * @param bo 访问日志对象
     */
    @Override
    public void insertLoginInfo(SysLoginInfoBo bo) {
        SysLoginInfo loginInfo = MapstructUtils.convert(bo, SysLoginInfo.class);
        loginInfo.setLoginTime(new Date());
        baseMapper.insert(loginInfo);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param loginInfo 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLoginInfoVo> selectLoginInfoList(SysLoginInfoBo loginInfo) {
        Map<String, Object> params = loginInfo.getParams();
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysLoginInfo>()
            .like(StringUtils.isNotBlank(loginInfo.getIpaddr()), SysLoginInfo::getIpaddr, loginInfo.getIpaddr())
            .eq(StringUtils.isNotBlank(loginInfo.getStatus()), SysLoginInfo::getStatus, loginInfo.getStatus())
            .like(StringUtils.isNotBlank(loginInfo.getUserName()), SysLoginInfo::getUserName, loginInfo.getUserName())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysLoginInfo::getLoginTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(SysLoginInfo::getInfoId));
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLoginInfoByIds(Long[] infoIds) {
        return baseMapper.deleteByIds(Arrays.asList(infoIds));
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLoginInfo() {
        baseMapper.delete(new LambdaQueryWrapper<>());
    }
}
