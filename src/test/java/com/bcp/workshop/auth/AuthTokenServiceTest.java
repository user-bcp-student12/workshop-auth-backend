package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsCompactHs256TokenWithSubjectAndNumericTimestamps() throws Exception {
        long now = 1_700_000_000L;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC);
        AuthTokenService service = new AuthTokenService(
                "test-only-secret-32-bytes-minimum",
                objectMapper,
                clock
        );

        String[] segments = service.create("student", "angular-workshop", "STUDENT").split("\\.");

        assertEquals(3, segments.length);
        JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[0]));
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
        assertEquals("HS256", header.get("alg").asText());
        assertEquals("JWT", header.get("typ").asText());
        assertEquals("student", claims.get("sub").asText());
        assertEquals("angular-workshop", claims.get("client").asText());
        assertEquals("STUDENT", claims.get("role").asText());
        assertTrue(claims.get("iat").isNumber());
        assertTrue(claims.get("exp").isNumber());
        assertEquals(now, claims.get("iat").asLong());
        assertEquals(now + AuthTokenService.TOKEN_TTL_SECONDS, claims.get("exp").asLong());
        long secondsUntilExpiry = claims.get("exp").asLong() - clock.instant().getEpochSecond();
        assertTrue(Math.abs(secondsUntilExpiry - AuthTokenService.TOKEN_TTL_SECONDS) <= 5);
    }

    @Test
    void rejectsChangesToHeaderPayloadOrSignature() {
        AuthTokenService service = new AuthTokenService(
                "test-only-secret-32-bytes-minimum",
                objectMapper,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC)
        );
        String[] tokenSegments = service.create("student", "angular-workshop", "STUDENT").split("\\.");

        for (int index = 0; index < tokenSegments.length; index++) {
            String[] alteredSegments = tokenSegments.clone();
            byte[] alteredSegment = Base64.getUrlDecoder().decode(alteredSegments[index]);
            alteredSegment[0] ^= 1;
            alteredSegments[index] = Base64.getUrlEncoder().withoutPadding().encodeToString(alteredSegment);
            String alteredToken = String.join(".", alteredSegments);

            assertThrows(IllegalArgumentException.class, () -> service.verify(alteredToken));
        }
    }

    @Test
    void rejectsExpiredToken() {
        long issuedAt = 1_700_000_000L;
        AuthTokenService issuer = new AuthTokenService(
                "test-only-secret-32-bytes-minimum",
                objectMapper,
                Clock.fixed(Instant.ofEpochSecond(issuedAt), ZoneOffset.UTC)
        );
        AuthTokenService verifier = new AuthTokenService(
                "test-only-secret-32-bytes-minimum",
                objectMapper,
                Clock.fixed(Instant.ofEpochSecond(issuedAt + AuthTokenService.TOKEN_TTL_SECONDS + 1), ZoneOffset.UTC)
        );

        String token = issuer.create("student", "angular-workshop", "STUDENT");

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(token));
    }
}