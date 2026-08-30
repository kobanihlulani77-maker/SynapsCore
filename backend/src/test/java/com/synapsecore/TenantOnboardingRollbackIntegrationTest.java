package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsecore.access.TenantOnboardingService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.access.dto.TenantOnboardingRequest;
import com.synapsecore.access.dto.TenantUserProvisioningRequest;
import com.synapsecore.access.dto.TenantWarehouseProvisioningRequest;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.config.SynapseStarterProperties;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.tenant.TenantContextService;
import org.springframework.boot.test.context.TestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=true",
    "synapsecore.starter.auto-seed-on-empty=false"
})
@ActiveProfiles("test")
@Import(TenantOnboardingRollbackIntegrationTest.RollbackFailureConfiguration.class)
class TenantOnboardingRollbackIntegrationTest {

    @Autowired
    private TenantOnboardingService tenantOnboardingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private IntegrationConnectorRepository integrationConnectorRepository;

    @Test
    void lateOnboardingFailureRollsBackTenantConfigurationAndChildren() {
        String tenantCode = "ROLLBACK-BOUNDARY";

        TenantOnboardingRequest request = new TenantOnboardingRequest(
            tenantCode,
            "Rollback Boundary Company",
            "Isolated rollback fixture.",
            "Rollback Tenant Admin",
            "rollback.admin",
            "Rollback-Admin-2026!",
            "Johannesburg",
            null,
            List.of(new TenantWarehouseProvisioningRequest(
                "ROLLBACK-DC",
                "Rollback Distribution Centre",
                "Johannesburg"
            )),
            List.of(new TenantUserProvisioningRequest(
                "rollback.admin",
                "Rollback Tenant Admin",
                "Rollback Tenant Admin",
                null,
                null,
                List.of(SynapseAccessRole.TENANT_ADMIN),
                List.of(),
                "Rollback-Admin-2026!"
            )),
            List.of()
        );

        assertThatThrownBy(() -> tenantOnboardingService.onboardTenant(request, "platform-owner"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("injected late provisioning failure");

        assertThat(tenantRepository.findByCodeIgnoreCase(tenantCode)).isEmpty();
        assertThat(warehouseRepository.findAll().stream()
            .noneMatch(warehouse -> tenantCode.equalsIgnoreCase(warehouse.getTenant().getCode())))
            .isTrue();
        assertThat(accessOperatorRepository.findAll().stream()
            .noneMatch(operator -> tenantCode.equalsIgnoreCase(operator.getTenant().getCode())))
            .isTrue();
        assertThat(accessUserRepository.findAll().stream()
            .noneMatch(user -> tenantCode.equalsIgnoreCase(user.getTenant().getCode())))
            .isTrue();
        assertThat(integrationConnectorRepository.findAll().stream()
            .noneMatch(connector -> tenantCode.equalsIgnoreCase(connector.getTenant().getCode())))
            .isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RollbackFailureConfiguration {

        @Bean
        @Primary
        IntegrationConnectorService failingIntegrationConnectorService(
            IntegrationConnectorRepository integrationConnectorRepository,
            AccessOperatorRepository accessOperatorRepository,
            AccessDirectoryService accessDirectoryService,
            IntegrationInboundRecordRepository integrationInboundRecordRepository,
            IntegrationImportRunRepository integrationImportRunRepository,
            IntegrationReplayRecordRepository integrationReplayRecordRepository,
            BusinessEventService businessEventService,
            AuditLogService auditLogService,
            OperationalStateChangePublisher operationalStateChangePublisher,
            TenantContextService tenantContextService,
            SynapseStarterProperties starterProperties,
            WarehouseRepository warehouseRepository
        ) {
            return new IntegrationConnectorService(
                integrationConnectorRepository,
                accessOperatorRepository,
                accessDirectoryService,
                integrationInboundRecordRepository,
                integrationImportRunRepository,
                integrationReplayRecordRepository,
                businessEventService,
                auditLogService,
                operationalStateChangePublisher,
                tenantContextService,
                starterProperties,
                warehouseRepository
            ) {
                @Override
                public void seedStarterConnectors(Tenant tenant) {
                    throw new IllegalStateException("injected late provisioning failure");
                }
            };
        }
    }
}
