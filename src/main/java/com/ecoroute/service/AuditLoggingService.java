package com.ecoroute.service;

import com.ecoroute.model.AuditLog;
import com.ecoroute.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLoggingService {

    private final AuditLogRepository auditLogRepository;

    public AuditLoggingService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a system action.
     * Transactional propagation is set to REQUIRES_NEW to ensure logs are written
     * even if the calling transaction fails or rolls back later.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String details, String username, String companyName) {
        AuditLog log = new AuditLog(action, details, username, companyName);
        auditLogRepository.save(log);
    }
}
