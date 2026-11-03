package org.dromara.quant.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.quant.account.domain.vo.TradeHistoryPageVo;
import org.dromara.quant.account.service.TradeHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开平仓记录只读接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/quant/trade-history")
public class TradeHistoryController {
    private final TradeHistoryService historyService;

    /**
     * 客户、交易对、开仓日期筛选与分页。
     */
    @SaCheckPermission("quant:trade:list")
    @GetMapping("/page")
    public R<TradeHistoryPageVo> page(PageQuery query, @RequestParam(required = false) Long credentialId,
                                     @RequestParam(required = false) String symbol, @RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
        return R.ok(historyService.page(query, credentialId, symbol, from, to));
    }
}
