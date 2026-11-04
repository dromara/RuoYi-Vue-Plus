package org.dromara.quant.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.quant.account.domain.vo.TradeCyclePageVo;
import org.dromara.quant.account.service.TradeCycleQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only trade history API backed by the persisted account-wide cycle ledger. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/quant/trade-history")
public class TradeCycleController {
    private final TradeCycleQueryService queryService;

    @SaCheckPermission("quant:trade:list")
    @GetMapping("/page")
    public R<TradeCyclePageVo> page(PageQuery query,
                                    @RequestParam(required = false) Long credentialId,
                                    @RequestParam(required = false) String symbol,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        return R.ok(queryService.page(query, credentialId, symbol, from, to));
    }
}
