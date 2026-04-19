package org.example.orderservice.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final String MDC_CLIENT_IP = "clientIp";

    public void logAuthSuccess(String email) {
        audit.info("AUTH_SUCCESS email={} ip={}", email, MDC.get(MDC_CLIENT_IP));
    }

    public void logAuthFailure(String email) {
        audit.warn("AUTH_FAILURE email={} ip={}", email, MDC.get(MDC_CLIENT_IP));
    }

    public void logAdminOperation(String adminId, String operation, String detail) {
        audit.info("ADMIN_OP operation={} adminId={} detail={} ip={}",
                operation, adminId, detail, MDC.get(MDC_CLIENT_IP));
    }
}
