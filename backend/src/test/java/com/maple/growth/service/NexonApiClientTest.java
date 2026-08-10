package com.maple.growth.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.nexon.NexonCharacterSnapshot;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NexonApiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void successfulLookupSendsApiKeyAndParsesSnapshot() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"ocid\":\"ocid-123\"}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("""
                {
                  "character_name": "Aries92",
                  "world_name": "루나",
                  "character_class": "나이트로드",
                  "character_level": "278",
                  "character_exp": "123456789",
                  "character_exp_rate": "42.1234",
                  "character_gender": "male",
                  "character_image": "https://example.com/image.png"
                }
                """).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("""
                {
                  "final_stat": [
                    {"stat_name": "전투력", "stat_value": "7420500"}
                  ]
                }
                """).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("""
                {
                  "hexamatrix": [
                    {"hexa_core_level": "1"},
                    {"hexa_core_level": "2"}
                  ]
                }
                """).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{\"union_level\":\"8500\"}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{\"union_artifact_level\":\"42\"}").addHeader("Content-Type", "application/json"));

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .defaultHeader("x-nxopen-api-key", "test-key")
                .build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        NexonCharacterSnapshot snapshot = client.fetchCharacterSnapshot("아리엘", LocalDate.of(2026, 8, 2));

        assertThat(snapshot.ocid()).isEqualTo("ocid-123");
        assertThat(snapshot.name()).isEqualTo("Aries92");
        assertThat(snapshot.level()).isEqualTo(278);
        assertThat(snapshot.exp()).isEqualTo(123456789L);
        assertThat(snapshot.expRate()).hasToString("42.1234");
        assertThat(snapshot.combatPower()).isEqualTo(7420500L);
        assertThat(snapshot.unionLevel()).isEqualTo(8500);
        assertThat(snapshot.unionArtifactLevel()).isEqualTo(42);
        assertThat(snapshot.hexaMatrixLevelSum()).isEqualTo(3);

        var request = server.takeRequest();
        assertThat(request.getHeader("x-nxopen-api-key")).isEqualTo("test-key");
        assertThat(request.getPath()).contains("character_name=%EC%95%84%EB%A6%AC%EC%97%98");
    }

    @Test
    void optionalEndpointFailuresReturnNullsInsteadOfFailingLookup() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"ocid\":\"ocid-123\"}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("""
                {
                  "character_name": "Aries92",
                  "world_name": "루나",
                  "character_class": "나이트로드",
                  "character_level": "278",
                  "character_exp": "123456789",
                  "character_exp_rate": "42.1234",
                  "character_gender": "male",
                  "character_image": "https://example.com/image.png"
                }
                """).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("""
                {
                  "final_stat": [
                    {"stat_name": "전투력", "stat_value": "7420500"}
                  ]
                }
                """).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(404));

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .defaultHeader("x-nxopen-api-key", "test-key")
                .build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        NexonCharacterSnapshot snapshot = client.fetchCharacterSnapshot("아리엘", LocalDate.of(2026, 8, 2));

        assertThat(snapshot.unionLevel()).isNull();
        assertThat(snapshot.unionArtifactLevel()).isNull();
        assertThat(snapshot.hexaMatrixLevelSum()).isNull();
        assertThat(snapshot.combatPower()).isEqualTo(7420500L);
    }

    @Test
    void notFoundMapsToCharacterNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("missing", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.CHARACTER_NOT_FOUND);
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    @Test
    void openApiInvalidParameterMapsToInvalidCharacterName() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":{\"name\":\"OPENAPI00004\",\"message\":\"Please input valid parameter\"}}")
                .addHeader("Content-Type", "application/json"));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("invalid", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.INVALID_CHARACTER_NAME);
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    @Test
    void openApiInvalidParameterFromDetailEndpointStaysGenericFailure() {
        server.enqueue(new MockResponse().setBody("{\"ocid\":\"ocid-123\"}").addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":{\"name\":\"OPENAPI00004\",\"message\":\"Please input valid parameter\"}}")
                .addHeader("Content-Type", "application/json"));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("Aries92", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.NEXON_API_FAILED);
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    @Test
    void openApiInvalidApiKeyMapsToDistinctAuthFailure() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":{\"name\":\"OPENAPI00005\",\"message\":\"Please input valid parameter\"}}")
                .addHeader("Content-Type", "application/json"));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("Aries92", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.NEXON_API_AUTH_FAILED);
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    @Test
    void rateLimitMapsToRetryable() {
        server.enqueue(new MockResponse().setResponseCode(429));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("rate-limited", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.RATE_LIMITED);
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    @Test
    void unavailableMapsToRetryableFailure() {
        server.enqueue(new MockResponse().setResponseCode(503));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("unavailable", LocalDate.of(2026, 8, 2)))
                .isInstanceOfSatisfying(NexonApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.NEXON_API_UNAVAILABLE);
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    @Test
    void timeoutMapsToRetryableFailure() {
        server.enqueue(new MockResponse()
                .setBody("{\"ocid\":\"ocid-123\"}")
                .setBodyDelay(2, TimeUnit.SECONDS)
                .addHeader("Content-Type", "application/json"));
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NexonApiClient client = new NexonApiClient(webClient, Duration.ofMillis(100));

        assertThatThrownBy(() -> client.fetchCharacterSnapshot("slow", LocalDate.of(2026, 8, 2)))
                .isInstanceOf(NexonApiException.class);
    }
}
