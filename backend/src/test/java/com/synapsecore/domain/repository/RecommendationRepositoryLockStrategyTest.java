package com.synapsecore.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.RecommendationStatus;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

class RecommendationRepositoryLockStrategyTest {

    @Test
    void conditionLockUsesDirectPessimisticLockWithoutFetchGraph() throws Exception {
        Method method = RecommendationRepository.class.getMethod(
            "findByTenantCodeAndConditionKeyForUpdate",
            String.class,
            String.class,
            RecommendationStatus.class
        );

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(method.getAnnotation(EntityGraph.class)).isNull();
    }
}
