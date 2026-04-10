package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    long countByCreatedAtAfter(Instant createdAt);

    long countByTenant_CodeIgnoreCaseAndCreatedAtAfter(String tenantCode, Instant createdAt);

    List<Recommendation> findTop12ByOrderByCreatedAtDesc();

    List<Recommendation> findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);

    Optional<Recommendation> findFirstByTypeAndTitleOrderByCreatedAtDesc(RecommendationType type, String title);

    Optional<Recommendation> findFirstByTenant_CodeIgnoreCaseAndTypeAndTitleOrderByCreatedAtDesc(String tenantCode,
                                                                                                  RecommendationType type,
                                                                                                  String title);
}
