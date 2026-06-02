package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.vo.SysConfigVo;
import org.dromara.system.service.ISysConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 参数配置 信息操作处理
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/config")
public class SysConfigController extends BaseController {

    private final ISysConfigService configService;

    /**
     * 分页查询参数配置列表。
     *
     * @param config    查询条件
     * @param pageQuery 分页参数
     * @return 参数配置分页结果
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public R<PageResult<SysConfigVo>> list(SysConfigBo config, PageQuery pageQuery) {
        return R.ok(configService.selectPageConfigList(config, pageQuery));
    }

    /**
     * 导出参数配置列表。
     *
     * @param config   查询条件
     * @param response HTTP 响应
     */
    @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:config:export")
    @PostMapping("/export")
    public void export(SysConfigBo config, HttpServletResponse response) {
        List<SysConfigVo> list = configService.selectConfigList(config);
        ExcelBuilder.of(list, SysConfigVo.class).sheetName("参数数据").toResponse(response);
    }

    /**
     * 根据参数编号获取详细信息
     *
     * @param configId 参数ID
     * @return 参数配置详情
     */
    @SaCheckPermission("system:config:query")
    @GetMapping(value = "/{configId}")
    public R<SysConfigVo> getInfo(@PathVariable Long configId) {
        return R.ok(configService.selectConfigById(configId));
    }

    /**
     * 根据参数键名查询参数值
     *
     * @param configKey 参数Key
     * @return 参数值
     */
    @GetMapping(value = "/configKey/{configKey}")
    public R<String> getConfigKey(@PathVariable String configKey) {
        return R.data(configService.selectConfigByKey(configKey));
    }

    /**
     * 新增参数配置。
     *
     * @param config 参数配置
     * @return 操作结果
     */
    @SaCheckPermission("system:config:add")
    @Log(title = "参数管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysConfigBo config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return R.fail("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.insertConfig(config);
        return R.ok();
    }

    /**
     * 修改参数配置。
     *
     * @param config 参数配置
     * @return 操作结果
     */
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysConfigBo config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return R.fail("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.updateConfig(config);
        return R.ok();
    }

    /**
     * 根据参数键名修改参数配置。
     *
     * @param config 参数配置
     * @return 操作结果
     */
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/updateByKey")
    public R<Void> updateByKey(@RequestBody SysConfigBo config) {
        configService.updateConfig(config);
        return R.ok();
    }

    /**
     * 删除参数配置
     *
     * @param configIds 参数ID串
     * @return 操作结果
     */
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable Long[] configIds) {
        configService.deleteConfigByIds(Arrays.asList(configIds));
        return R.ok();
    }

    /**
     * 刷新参数缓存。
     *
     * @return 操作结果
     */
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.CLEAN)
    @DeleteMapping("/refreshCache")
    public R<Void> refreshCache() {
        configService.resetConfigCache();
        return R.ok();
    }
}
