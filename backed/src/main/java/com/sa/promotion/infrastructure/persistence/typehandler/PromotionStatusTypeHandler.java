package com.sa.promotion.infrastructure.persistence.typehandler;

import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(PromotionStatus.class)
public class PromotionStatusTypeHandler extends BaseTypeHandler<PromotionStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PromotionStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public PromotionStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : PromotionStatus.fromCode(code);
    }

    @Override
    public PromotionStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : PromotionStatus.fromCode(code);
    }

    @Override
    public PromotionStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : PromotionStatus.fromCode(code);
    }
}
