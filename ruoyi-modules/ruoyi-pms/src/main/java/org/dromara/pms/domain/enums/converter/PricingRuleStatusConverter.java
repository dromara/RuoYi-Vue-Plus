package org.dromara.pms.domain.enums.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.dromara.pms.domain.enums.PricingRuleStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 价格规则状态枚举转换器
 *
 * @author ruoyi
 * @date 2024-12-19
 */
public class PricingRuleStatusConverter extends BaseTypeHandler<PricingRuleStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PricingRuleStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public PricingRuleStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : PricingRuleStatus.fromCode(code);
    }

    @Override
    public PricingRuleStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : PricingRuleStatus.fromCode(code);
    }

    @Override
    public PricingRuleStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : PricingRuleStatus.fromCode(code);
    }
}