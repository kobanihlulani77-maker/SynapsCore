package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    long countByStatus(AlertStatus status);

    long countByTenant_CodeIgnoreCaseAndStatus(String tenantCode, AlertStatus status);

    List<Alert> findTop12ByOrderByUpdatedAtDesc();

    List<Alert> findTop12ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product"})
    List<Alert> findAllByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(String tenantCode);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product"})
    List<Alert> findAllByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(String tenantCode, AlertStatus status);

    List<Alert> findTop12ByStatusOrderByCreatedAtDesc(AlertStatus status);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product"})
    List<Alert> findTop12ByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(String tenantCode, AlertStatus status);

    long countByTenant_CodeIgnoreCaseAndWarehouse_CodeIgnoreCaseAndStatus(String tenantCode,
                                                                            String warehouseCode,
                                                                            AlertStatus status);

    Optional<Alert> findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(String tenantCode,
                                                                                     AlertType type,
                                                                                     AlertStatus status,
                                                                                     String conditionKey);

    @Query("""
        select a.conditionKey
        from Alert a
        where a.status = :status
          and a.conditionKey is not null
        """)
    List<String> findConditionKeysByStatus(@Param("status") AlertStatus status);
}
