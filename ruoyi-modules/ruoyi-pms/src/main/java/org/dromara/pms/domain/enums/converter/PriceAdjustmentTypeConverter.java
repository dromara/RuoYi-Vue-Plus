package org.dromara.pms.domain.enums.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.dromara.pms.domain.enums.PriceAdjustmentType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 价格调整类型枚举转换器
 *
 * @author ruoyi
 * @date 2024-12-19
 */
public class PriceAdjustmentTypeConverter extends BaseTypeHandler<PriceAdjustmentType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PriceAdjustmentType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public PriceAdjustmentType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : PriceAdjustmentType.fromCode(code);
    }

    @Override
    public PriceAdjustmentType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : PriceAdjustmentType.fromCode(code);
    }

    @Override
    public PriceAdjustmentType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : PriceAdjustmentType.fromCode(code);
    }
}
