package com.maple.growth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionPropertiesTest {

    @Test
    void defaultsProvideBoundedRetrySettings() {
        CollectionProperties properties = new CollectionProperties(null, null, null, null, null);

        assertThat(properties.retryCron()).isEqualTo("0 */15 * * * *");
        assertThat(properties.retryBatchSize()).isEqualTo(20);
        assertThat(properties.retryMaxAttempts()).isEqualTo(3);
        assertThat(properties.retryInitialBackoffSeconds()).isEqualTo(300);
        assertThat(properties.retryLeaseSeconds()).isEqualTo(900);
    }

    @Test
    void acceptsExplicitSettings() {
        CollectionProperties properties = new CollectionProperties("0 */5 * * * *", 10, 4, 60, 600);

        assertThat(properties.retryCron()).isEqualTo("0 */5 * * * *");
        assertThat(properties.retryBatchSize()).isEqualTo(10);
        assertThat(properties.retryMaxAttempts()).isEqualTo(4);
    }

    @Test
    void rejectsNonPositiveExplicitSettings() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new CollectionProperties("0 */15 * * * *", 0, 3, 60, 900)
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new CollectionProperties("0 */15 * * * *", 20, 3, 60, 0)
        );
    }
}
