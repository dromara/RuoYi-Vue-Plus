package org.dromara.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.vo.SysLoginInfoVo;
import org.dromara.system.service.ISysLoginInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统访问记录
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/loginInfo")
public class SysLoginInfoController extends BaseController {

    private final ISysLoginInfoService loginInfoService;

    /**
     * 获取系统访问记录列表
     */
    @SaCheckPermission("monitor:logininfo:list")
    @GetMapping("/list")
    public TableDataInfo<SysLoginInfoVo> list(SysLoginInfoBo loginInfo, PageQuery pageQuery) {
        return loginInfoService.selectPageLoginInfoList(loginInfo, pageQuery);
    }

    /**
     * 导出系统访问记录列表
     */
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("monitor:logininfo:export")
    @PostMapping("/export")
    public void export(SysLoginInfoBo loginInfo, HttpServletResponse response) {
        List<SysLoginInfoVo> list = loginInfoService.selectLoginInfoList(loginInfo);
        ExcelUtil.exportExcel(list, "登录日志", SysLoginInfoVo.class, response);
    }

    /**
     * 批量删除登录日志
     * @param infoIds 日志ids
     */
    @SaCheckPermission("monitor:logininfo:remove")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public R<Void> remove(@PathVariable Long[] infoIds) {
        return toAjax(loginInfoService.deleteLoginInfoByIds(infoIds));
    }

    /**
     * 清理系统访问记录
     */
    @SaCheckPermission("monitor:logininfo:remove")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @Lock4j
    @DeleteMapping("/clean")
    public R<Void> clean() {
        loginInfoService.cleanLoginInfo();
        return R.ok();
    }

    @SaCheckPermission("monitor:logininfo:unlock")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @GetMapping("/unlock/{userName}")
    public R<Void> unlock(@PathVariable("userName") String userName) {
        String loginName = CacheConstants.PWD_ERR_CNT_KEY + userName;
        if (RedisUtils.hasKey(loginName)) {
            RedisUtils.deleteObject(loginName);
        }
        return R.ok();
    }

}
