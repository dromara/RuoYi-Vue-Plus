package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysDictTypeBo;
import org.dromara.system.domain.vo.SysDictTypeVo;
import org.dromara.system.service.ISysDictTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 数据字典信息
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController extends BaseController {

    private final ISysDictTypeService dictTypeService;

    /**
     * 分页查询字典类型列表。
     *
     * @param dictType 查询条件
     * @param pageQuery 分页参数
     * @return 字典类型分页结果
     */
    @SaCheckPermission("system:dict:list")
    @GetMapping("/list")
    public R<PageResult<SysDictTypeVo>> list(SysDictTypeBo dictType, PageQuery pageQuery) {
        return R.ok(dictTypeService.selectPageDictTypeList(dictType, pageQuery));
    }

    /**
     * 导出字典类型列表。
     *
     * @param dictType 查询条件
     * @param response HTTP 响应
     */
    @Log(title = "字典类型", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:dict:export")
    @PostMapping("/export")
    public void export(SysDictTypeBo dictType, HttpServletResponse response) {
        List<SysDictTypeVo> list = dictTypeService.selectDictTypeList(dictType);
        ExcelUtil.exportExcel(list, "字典类型", SysDictTypeVo.class, response);
    }

    /**
     * 查询字典类型详细
     *
     * @param dictId 字典ID
     * @return 字典类型详情
     */
    @SaCheckPermission("system:dict:query")
    @GetMapping(value = "/{dictId}")
    public R<SysDictTypeVo> getInfo(@PathVariable Long dictId) {
        return R.ok(dictTypeService.selectDictTypeById(dictId));
    }

    /**
     * 新增字典类型。
     *
     * @param dict 字典类型参数
     * @return 操作结果
     */
    @SaCheckPermission("system:dict:add")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysDictTypeBo dict) {
        if (!dictTypeService.checkDictTypeUnique(dict)) {
            return R.fail("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dictTypeService.insertDictType(dict);
        return R.ok();
    }

    /**
     * 修改字典类型。
     *
     * @param dict 字典类型参数
     * @return 操作结果
     */
    @SaCheckPermission("system:dict:edit")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysDictTypeBo dict) {
        if (!dictTypeService.checkDictTypeUnique(dict)) {
            return R.fail("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dictTypeService.updateDictType(dict);
        return R.ok();
    }

    /**
     * 删除字典类型
     *
     * @param dictIds 字典ID串
     * @return 操作结果
     */
    @SaCheckPermission("system:dict:remove")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictIds}")
    public R<Void> remove(@PathVariable Long[] dictIds) {
        dictTypeService.deleteDictTypeByIds(Arrays.asList(dictIds));
        return R.ok();
    }

    /**
     * 刷新字典缓存。
     *
     * @return 操作结果
     */
    @SaCheckPermission("system:dict:remove")
    @Log(title = "字典类型", businessType = BusinessType.CLEAN)
    @Lock4j
    @DeleteMapping("/refreshCache")
    public R<Void> refreshCache() {
        dictTypeService.resetDictCache();
        return R.ok();
    }

    /**
     * 获取字典类型下拉选择列表。
     *
     * @return 字典类型列表
     */
    @GetMapping("/optionselect")
    public R<List<SysDictTypeVo>> optionselect() {
        List<SysDictTypeVo> dictTypes = dictTypeService.selectDictTypeAll();
        return R.ok(dictTypes);
    }
}
