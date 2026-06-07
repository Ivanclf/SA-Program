package com.sa.promotion.infrastructure.persistence.typehandler;

import com.sa.promotion.domain.event.enums.EventType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * EventType 枚举 ↔ VARCHAR 字段的 MyBatis TypeHandler
 *
 * EventType 以 String code（如 "E_CREATE_DRAFT"）存储在数据库 VARCHAR 列中，
 * 本 Handler 负责读写时的双向转换。
 */
@MappedTypes(EventType.class)
public class EventTypeTypeHandler extends BaseTypeHandler<EventType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, EventType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public EventType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return rs.wasNull() ? null : EventType.fromCode(code);
    }

    @Override
    public EventType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return rs.wasNull() ? null : EventType.fromCode(code);
    }

    @Override
    public EventType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return cs.wasNull() ? null : EventType.fromCode(code);
    }
}
