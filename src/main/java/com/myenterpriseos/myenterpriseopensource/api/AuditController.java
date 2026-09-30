package com.myenterpriseos.myenterpriseopensource.api;

import com.myenterpriseos.myenterpriseopensource.service.AuditService;
import com.myenterpriseos.myenterpriseopensource.service.AuditService.AuditPageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-events")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AuditPageResponse list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        return audit.list(page, size);
    }
}
