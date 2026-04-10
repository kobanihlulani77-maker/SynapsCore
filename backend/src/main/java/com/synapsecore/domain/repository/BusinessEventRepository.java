package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessEventRepository extends JpaRepository<BusinessEvent, Long> {

    List<BusinessEvent> findTop20ByOrderByCreatedAtDesc();

    List<BusinessEvent> findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);

    long countByCreatedAtAfter(Instant createdAt);

    long countByTenantCodeIgnoreCaseAndCreatedAtAfter(String tenantCode, Instant createdAt);

    long countByEventTypeInAndCreatedAtAfter(Collection<BusinessEventType> eventTypes, Instant createdAt);

    long countByTenantCodeIgnoreCaseAndEventTypeInAndCreatedAtAfter(String tenantCode,
                                                                    Collection<BusinessEventType> eventTypes,
                                                                    Instant createdAt);

    Optional<BusinessEvent> findTopByOrderByCreatedAtDesc();

    Optional<BusinessEvent> findTopByTenantCodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);
}
