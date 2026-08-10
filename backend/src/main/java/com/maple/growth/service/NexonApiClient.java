package com.maple.growth.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.nexon.NexonCharacterSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class NexonApiClient {

    private static final Logger log = LoggerFactory.getLogger(NexonApiClient.class);

    private final WebClient nexonWebClient;
    private final Duration nexonTimeout;

    public NexonCharacterSnapshot fetchCharacterSnapshot(String characterName, LocalDate snapshotDate) {
        try {
            JsonNode ocidPayload = getJson("/maplestory/v1/id", "character_name", characterName);
            String ocid = requiredText(ocidPayload, "ocid");
            JsonNode basicPayload = getJson("/maplestory/v1/character/basic", "ocid", ocid);
            JsonNode statPayload = getJson("/maplestory/v1/character/stat", "ocid", ocid);
            JsonNode equipmentPayload = getOptionalJson("/maplestory/v1/character/item-equipment", "ocid", ocid);
            JsonNode hexaPayload = getOptionalJson("/maplestory/v1/character/hexa-core-equipment", "ocid", ocid);
            JsonNode unionPayload = getOptionalJson("/maplestory/v1/user/union", "ocid", ocid);
            JsonNode unionArtifactPayload = getOptionalJson("/maplestory/v1/user/union-artifact", "ocid", ocid);

            return new NexonCharacterSnapshot(
                    ocid,
                    textOrNull(basicPayload, "character_name", "characterName"),
                    textOrNull(basicPayload, "world_name", "worldName"),
                    textOrNull(basicPayload, "character_class", "job_name", "jobName"),
                    textOrNull(basicPayload, "character_gender", "gender"),
                    textOrNull(basicPayload, "character_image", "characterImage", "image_url", "imageUrl"),
                    intOrZero(basicPayload, "character_level", "level"),
                    longOrZero(basicPayload, "character_exp", "exp"),
                    decimalOrNull(basicPayload, "character_exp_rate", "exp_rate", "expRate"),
                    extractLongStat(statPayload, "전투력", "combat power"),
                    extractInt(unionPayload, "union_level", "unionLevel"),
                    extractInt(unionArtifactPayload, "union_artifact_level", "unionArtifactLevel"),
                    extractHexaLevelSum(hexaPayload),
                    statPayload,
                    equipmentPayload,
                    hexaPayload
            );
        } catch (NexonApiException e) {
            log.warn(
                    "Nexon snapshot fetch failed. characterName={}, snapshotDate={}, errorCode={}, retryable={}, message={}",
                    characterName,
                    snapshotDate,
                    e.getErrorCode(),
                    e.isRetryable(),
                    e.getMessage()
            );
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "Unexpected Nexon snapshot failure. characterName={}, snapshotDate={}, exceptionType={}, message={}",
                    characterName,
                    snapshotDate,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            throw new NexonApiException(ApiErrorCode.NEXON_API_FAILED, "Nexon API 호출에 실패했습니다.", true);
        }
    }

    private JsonNode getJson(String path, String paramName, String paramValue) {
        try {
            return nexonWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam(paramName, paramValue).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(nexonTimeout);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw mapHttpException(path, paramName, e);
        } catch (Exception e) {
            throw new NexonApiException(ApiErrorCode.NEXON_API_FAILED, "Nexon API 호출이 시간 초과되었습니다.", true);
        }
    }

    private JsonNode getOptionalJson(String path, String paramName, String paramValue) {
        try {
            return getJson(path, paramName, paramValue);
        } catch (WebClientResponseException | NexonApiException exception) {
            return null;
        }
    }

    private NexonApiException mapHttpException(String path, String paramName, org.springframework.web.reactive.function.client.WebClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String openApiErrorName = extractOpenApiErrorName(exception.getResponseBodyAsString());
        log.warn(
                "Nexon HTTP error. path={}, paramName={}, status={}, errorCode={}",
                path,
                paramName,
                status.value(),
                openApiErrorName == null ? "UNKNOWN" : openApiErrorName
        );
        if ("OPENAPI00004".equals(openApiErrorName) && "/maplestory/v1/id".equals(path) && "character_name".equals(paramName)) {
            return new NexonApiException(ApiErrorCode.INVALID_CHARACTER_NAME, "캐릭터명을 다시 확인해 주세요.", false);
        }
        if ("OPENAPI00005".equals(openApiErrorName)) {
            return new NexonApiException(ApiErrorCode.NEXON_API_AUTH_FAILED, "Nexon API 키가 유효하지 않습니다.", false);
        }
        if (status.value() == 404) {
            return new NexonApiException(ApiErrorCode.CHARACTER_NOT_FOUND, "캐릭터를 찾을 수 없습니다.", false);
        }
        if (status.value() == 429) {
            return new NexonApiException(ApiErrorCode.RATE_LIMITED, "호출 제한에 걸렸습니다.", true);
        }
        if (status.value() == 503) {
            return new NexonApiException(ApiErrorCode.NEXON_API_UNAVAILABLE, "Nexon API를 사용할 수 없습니다.", true);
        }
        if (status.is5xxServerError()) {
            return new NexonApiException(ApiErrorCode.NEXON_API_FAILED, "Nexon API 호출이 실패했습니다.", true);
        }
        return new NexonApiException(ApiErrorCode.NEXON_API_FAILED, "Nexon API 호출이 실패했습니다.", true);
    }

    private static String extractOpenApiErrorName(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        int errorIndex = responseBody.indexOf("\"name\"");
        if (errorIndex < 0) {
            return null;
        }
        int valueStart = responseBody.indexOf('"', errorIndex + 6);
        if (valueStart < 0) {
            return null;
        }
        int valueEnd = responseBody.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }
        String value = responseBody.substring(valueStart + 1, valueEnd);
        return value.startsWith("OPENAPI") ? value : null;
    }

    private static String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null || value.isBlank()) {
            throw new NexonApiException(ApiErrorCode.NEXON_API_FAILED, "Nexon API 응답 형식이 올바르지 않습니다.", true);
        }
        return value;
    }

    private static String textOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node == null ? null : node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static int intOrZero(JsonNode node, String... fields) {
        String text = textOrNull(node, fields);
        return text == null ? 0 : Integer.parseInt(text);
    }

    private static long longOrZero(JsonNode node, String... fields) {
        String text = textOrNull(node, fields);
        return text == null ? 0L : Long.parseLong(text);
    }

    private static BigDecimal decimalOrNull(JsonNode node, String... fields) {
        String text = textOrNull(node, fields);
        return text == null ? null : new BigDecimal(text);
    }

    private static Long extractLongStat(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        JsonNode array = node.get("final_stat");
        if (array == null || !array.isArray()) {
            return null;
        }
        for (JsonNode item : array) {
            String name = textOrNull(item, "stat_name", "name");
            if (name == null) {
                continue;
            }
            for (String candidate : names) {
                if (name.contains(candidate)) {
                    String value = textOrNull(item, "stat_value", "value");
                    if (value != null) {
                        return Long.parseLong(value.replace(",", ""));
                    }
                }
            }
        }
        return null;
    }

    private static Integer extractInt(JsonNode node, String... fields) {
        String text = textOrNull(node, fields);
        return text == null ? null : Integer.parseInt(text);
    }

    private static Integer extractHexaLevelSum(JsonNode node) {
        if (node == null) {
            return null;
        }
        JsonNode array = node.get("hexamatrix");
        if (array == null || !array.isArray()) {
            array = node.get("character_hexa_core_equipment");
        }
        if (array == null || !array.isArray()) {
            return null;
        }
        int sum = 0;
        boolean seen = false;
        for (JsonNode item : array) {
            String levelText = textOrNull(item, "hexa_core_level", "hexaLevel", "level");
            if (levelText == null) {
                continue;
            }
            sum += Integer.parseInt(levelText);
            seen = true;
        }
        return seen ? sum : null;
    }

}
