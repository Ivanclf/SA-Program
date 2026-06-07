package com.sa.promotion.infrastructure.persistence.typehandler;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(AuditStatus.class)
public class AuditStatusTypeHandler extends BaseTypeHandler<AuditStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AuditStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public AuditStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : AuditStatus.fromCode(code);
    }

    @Override
    public AuditStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : AuditStatus.fromCode(code);
    }

    @Override
    public AuditStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : AuditStatus.fromCode(code);
    }
}
