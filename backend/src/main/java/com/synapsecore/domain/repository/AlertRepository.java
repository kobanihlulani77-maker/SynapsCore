package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    long countByStatus(AlertStatus status);

    long countByTenant_CodeIgnoreCaseAndStatus(String tenantCode, AlertStatus status);

    List<Alert> findTop12ByOrderByUpdatedAtDesc();

    List<Alert> findTop12ByOrderByCreatedAtDesc();

    List<Alert> findTop12ByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(String tenantCode);

    List<Alert> findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);

    List<Alert> findTop12ByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findTop12ByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(String tenantCode, AlertStatus status);

    Optional<Alert> findFirstByTypeAndStatusAndTitle(AlertType type, AlertStatus status, String title);

    Optional<Alert> findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(String tenantCode,
                                                                              AlertType type,
                                                                              AlertStatus status,
                                                                              String title);
}
