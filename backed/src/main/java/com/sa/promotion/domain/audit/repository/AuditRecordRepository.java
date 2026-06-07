package com.sa.promotion.domain.audit.repository;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 审核记录仓储接口 (MyBatis Mapper)
 * 数据来源于 audit_record 表
 */
@Mapper
public interface AuditRecordRepository {
    void save(AuditRecord auditRecord);
    void update(AuditRecord auditRecord);
    Optional<AuditRecord> findById(String auditId);
    Optional<AuditRecord> findByPromotionId(String promotionId);
    List<AuditRecord> findAll();
    boolean exists(String promotionId);
}
