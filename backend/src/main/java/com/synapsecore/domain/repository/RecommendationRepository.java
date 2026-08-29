package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.entity.RecommendationType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    long countByTenant_CodeIgnoreCaseAndStatus(String tenantCode, RecommendationStatus status);

    List<Recommendation> findTop12ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product", "sourceWarehouse", "destinationWarehouse"})
    List<Recommendation> findAllByTenant_CodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(String tenantCode,
                                                                                      RecommendationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000"))
    @EntityGraph(attributePaths = {"tenant", "warehouse", "product", "sourceWarehouse", "destinationWarehouse"})
    @Query("""
        select r
        from Recommendation r
        where upper(r.tenant.code) = upper(:tenantCode)
          and r.conditionKey = :conditionKey
          and r.status = :status
        """)
    Optional<Recommendation> findByTenantCodeAndConditionKeyForUpdate(@Param("tenantCode") String tenantCode,
                                                                       @Param("conditionKey") String conditionKey,
                                                                       @Param("status") RecommendationStatus status);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product", "sourceWarehouse", "destinationWarehouse"})
    List<Recommendation> findAllByTenant_CodeIgnoreCaseAndSourceTypeAndSourceRefAndStatus(
        String tenantCode, String sourceType, String sourceRef, RecommendationStatus status);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "product", "sourceWarehouse", "destinationWarehouse"})
    List<Recommendation> findAllByTenant_CodeIgnoreCaseAndTypeAndProduct_IdAndSourceWarehouse_IdAndStatus(
        String tenantCode, RecommendationType type, Long productId, Long sourceWarehouseId, RecommendationStatus status);
}
