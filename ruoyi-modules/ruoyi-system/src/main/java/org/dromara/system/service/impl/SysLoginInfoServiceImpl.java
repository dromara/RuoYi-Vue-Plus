package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.common.mybatis.core.page.PageQuery;
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

import java.time.LocalDateTime;
import java.util.Arrays;
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

    private final SysLoginInfoMapper loginInfoMapper;

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
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientId = request.getHeader(LoginHelper.CLIENT_KEY);
        SysClientVo client = null;
        if (StringUtils.isNotBlank(clientId)) {
            client = clientService.queryByClientId(clientId);
        }

        String address = AddressUtils.getRealAddressByIP(ip);
        String s = getBlock(ip) +
            address +
            getBlock(loginInfoEvent.getUsername()) +
            getBlock(loginInfoEvent.getStatus()) +
            getBlock(loginInfoEvent.getMessage());
        // 打印信息到日志
        log.info(s, loginInfoEvent.getArgs());
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
        return "[" + msg + "]";
    }

    /**
     * 分页查询登录日志列表
     *
     * @param loginInfo 查询条件
     * @param pageQuery 分页参数
     * @return 登录日志分页列表
     */
    @Override
    public PageResult<SysLoginInfoVo> selectPageLoginInfoList(SysLoginInfoBo loginInfo, PageQuery pageQuery) {
        LambdaCrudChainWrapper<SysLoginInfo, SysLoginInfoVo> lqw = buildQueryWrapper(loginInfo);
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            lqw.orderByDesc(SysLoginInfo::getInfoId);
        }
        Page<SysLoginInfoVo> page = loginInfoMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 新增系统登录日志
     *
     * @param bo 访问日志对象
     */
    @Override
    public void insertLoginInfo(SysLoginInfoBo bo) {
        SysLoginInfo loginInfo = MapstructUtils.convert(bo, SysLoginInfo.class);
        loginInfo.setLoginTime(LocalDateTime.now());
        loginInfoMapper.insert(loginInfo);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param loginInfo 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLoginInfoVo> selectLoginInfoList(SysLoginInfoBo loginInfo) {
        return loginInfoMapper.selectVoList(buildQueryWrapper(loginInfo)
            .orderByDesc(SysLoginInfo::getInfoId));
    }

    /**
     * 构造登录日志列表查询条件。
     *
     * @param loginInfo 登录日志筛选条件
     * @return 登录日志查询包装器
     */
    private LambdaCrudChainWrapper<SysLoginInfo, SysLoginInfoVo> buildQueryWrapper(SysLoginInfoBo loginInfo) {
        Map<String, Object> params = loginInfo.getParams();
        return loginInfoMapper.lambda()
            .likeIfText(SysLoginInfo::getIpaddr, loginInfo.getIpaddr())
            .eqIfText(SysLoginInfo::getStatus, loginInfo.getStatus())
            .likeIfText(SysLoginInfo::getUserName, loginInfo.getUserName())
            .betweenParams(SysLoginInfo::getLoginTime, params, "beginTime", "endTime");
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLoginInfoByIds(Long[] infoIds) {
        return loginInfoMapper.deleteByIds(Arrays.asList(infoIds));
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLoginInfo() {
        loginInfoMapper.lambda().delete();
    }
}
