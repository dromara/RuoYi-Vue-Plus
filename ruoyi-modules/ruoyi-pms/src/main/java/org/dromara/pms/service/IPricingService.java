package org.dromara.pms.service;

import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.PmsPricingCalculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 价格计算服务接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface IPricingService {

    /**
     * 价格计算请求参数
     */
    class PricingRequest {
        private Long roomTypeId;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private Integer numAdults;
        private Integer numChildren;
        private String channelCode;
        private String memberLevel;
        private BigDecimal basePrice;
        private String calculationSource;
        private Long orderId;
        private Boolean isFinalBooking;

        // Getters and Setters
        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public LocalDate getCheckInDate() {
            return checkInDate;
        }

        public void setCheckInDate(LocalDate checkInDate) {
            this.checkInDate = checkInDate;
        }

        public LocalDate getCheckOutDate() {
            return checkOutDate;
        }

        public void setCheckOutDate(LocalDate checkOutDate) {
            this.checkOutDate = checkOutDate;
        }

        public Integer getNumAdults() {
            return numAdults;
        }

        public void setNumAdults(Integer numAdults) {
            this.numAdults = numAdults;
        }

        public Integer getNumChildren() {
            return numChildren;
        }

        public void setNumChildren(Integer numChildren) {
            this.numChildren = numChildren;
        }

        public String getChannelCode() {
            return channelCode;
        }

        public void setChannelCode(String channelCode) {
            this.channelCode = channelCode;
        }

        public String getMemberLevel() {
            return memberLevel;
        }

        public void setMemberLevel(String memberLevel) {
            this.memberLevel = memberLevel;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public String getCalculationSource() {
            return calculationSource;
        }

        public void setCalculationSource(String calculationSource) {
            this.calculationSource = calculationSource;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Boolean getIsFinalBooking() {
            return isFinalBooking;
        }

        public void setIsFinalBooking(Boolean isFinalBooking) {
            this.isFinalBooking = isFinalBooking;
        }
    }

    /**
     * 价格计算结果
     */
    class PricingResult {
        private BigDecimal basePrice;
        private BigDecimal finalPrice;
        private BigDecimal totalDiscount;
        private BigDecimal discountRate;
        private List<Map<String, Object>> appliedRules;
        private Map<String, Object> calculationContext;
        private Boolean success;
        private String errorMessage;

        // Getters and Setters
        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public BigDecimal getFinalPrice() {
            return finalPrice;
        }

        public void setFinalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
        }

        public BigDecimal getTotalDiscount() {
            return totalDiscount;
        }

        public void setTotalDiscount(BigDecimal totalDiscount) {
            this.totalDiscount = totalDiscount;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public void setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
        }

        public List<Map<String, Object>> getAppliedRules() {
            return appliedRules;
        }

        public void setAppliedRules(List<Map<String, Object>> appliedRules) {
            this.appliedRules = appliedRules;
        }

        public Map<String, Object> getCalculationContext() {
            return calculationContext;
        }

        public void setCalculationContext(Map<String, Object> calculationContext) {
            this.calculationContext = calculationContext;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 计算房间价格
     *
     * @param request 价格计算请求参数
     * @return 价格计算结果
     */
    PricingResult calculatePrice(PricingRequest request);

    /**
     * 批量计算价格
     *
     * @param requests 价格计算请求列表
     * @return 价格计算结果列表
     */
    List<PricingResult> batchCalculatePrice(List<PricingRequest> requests);

    /**
     * 获取适用的价格规则
     *
     * @param request 价格计算请求参数
     * @return 适用的价格规则列表
     */
    List<PmsRoomPricingRule> getApplicableRules(PricingRequest request);

    /**
     * 测试价格规则
     *
     * @param ruleId  规则ID
     * @param request 测试请求参数
     * @return 测试结果
     */
    PricingResult testRule(Long ruleId, PricingRequest request);

    /**
     * 模拟价格计算
     *
     * @param request       价格计算请求参数
     * @param simulateRules 模拟的规则列表
     * @return 模拟计算结果
     */
    PricingResult simulateCalculation(PricingRequest request, List<PmsRoomPricingRule> simulateRules);

    /**
     * 保存价格计算历史
     *
     * @param request 价格计算请求参数
     * @param result  价格计算结果
     * @return 保存的计算历史记录
     */
    PmsPricingCalculation savePricingHistory(PricingRequest request, PricingResult result);

    /**
     * 获取价格趋势分析
     *
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 价格趋势数据
     */
    Map<String, Object> getPriceTrendAnalysis(Long roomTypeId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取规则效果分析
     *
     * @param ruleId    规则ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 规则效果分析数据
     */
    Map<String, Object> getRuleEffectAnalysis(Long ruleId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取收益分析
     *
     * @param deptId    部门ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收益分析数据
     */
    Map<String, Object> getRevenueAnalysis(Long deptId, LocalDate startDate, LocalDate endDate);

    /**
     * 验证价格规则配置
     *
     * @param rule 价格规则
     * @return 验证结果
     */
    Map<String, Object> validateRule(PmsRoomPricingRule rule);

    /**
     * 复制价格规则
     *
     * @param sourceRuleId 源规则ID
     * @param newRuleName  新规则名称
     * @return 复制的规则
     */
    PmsRoomPricingRule copyRule(Long sourceRuleId, String newRuleName);
}
