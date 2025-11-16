package com.marketplace.out.repository;

import com.marketplace.model.AuditEvent;
import java.util.List;

public interface AuditRepository {
    AuditEvent save(AuditEvent event);
    List<AuditEvent> findAll();
}
