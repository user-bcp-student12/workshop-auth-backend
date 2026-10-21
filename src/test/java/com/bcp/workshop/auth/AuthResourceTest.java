package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue())
                .body("expiresIn", is(3600))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("debug", is("authenticated-with-legacy-flow"));
    }

    @Test
        void acceptsLoginWhenClientIsAbsent() throws Exception {
        Map<?, ?> responseBody = given()
                .contentType("application/json")
                .body(Map.of("username", "student", "password", "Workshop123!"))
                .when().post("/auth/login")
                .then()
                .statusCode(200)
            .body("token", notNullValue())
            .extract()
            .as(Map.class);

        String token = (String) responseBody.get("token");
        String[] tokenSegments = token.split("\\.");
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(tokenSegments[1]));

        assertEquals("angular-workshop", claims.get("client").asText());
        }

        @Test
        void rejectsClientOutsideConfiguredAllowListWithoutToken() {
        given()
            .contentType("application/json")
            .body(Map.of(
                "username", "student",
                "password", "Workshop123!",
                "client", "unapproved-client"
            ))
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Client is not allowed"))
            .body("token", nullValue());
    }

    @Test
    void expiresInMatchesJwtLifetime() throws Exception {
        Map<?, ?> responseBody = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .as(Map.class);

        String token = (String) responseBody.get("token");
        String[] tokenSegments = token.split("\\.");
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(tokenSegments[1]));
        long expiresIn = ((Number) responseBody.get("expiresIn")).longValue();

        assertEquals(expiresIn, claims.get("exp").asLong() - claims.get("iat").asLong());
    }

    @Test
    void rejectsEmptyOrWhitespaceUsername() {
        for (String username : List.of("", "   ")) {
            assertLoginError(
                    Map.of("username", username, "password", "Workshop123!"),
                    400,
                    "Username is required"
            );
        }
    }

    @Test
    void rejectsEmptyOrWhitespacePassword() {
        for (String password : List.of("", "   ")) {
            assertLoginError(
                    Map.of("username", "student", "password", password),
                    400,
                    "Password is required"
            );
        }
    }

    @Test
    void rejectsEmptyRequestWithExactMessage() {
        given()
                .contentType("application/json")
                .body("")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsInvalidCredentialsWithExactMessage() {
        assertLoginError(
                Map.of("username", "student", "password", "invalid-password"),
                401,
                "Invalid user student or password"
        );
    }

    @Test
    void escapesDisplayNameHtml() {
        AuthResource resource = new AuthResource();
        resource.tokenService = new AuthTokenService(
            "test-only-secret-32-bytes-minimum",
            new ObjectMapper(),
            Clock.systemUTC()
        );
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1, "html-user", "<b>Workshop & Student</b>", "STUDENT");
            }
        };
        LoginRequest request = new LoginRequest();
        request.username = "html-user";
        request.password = "test-password";

        try (Response response = resource.login(request)) {
            assertEquals(200, response.getStatus());
            Object responseEntity = response.getEntity();
            assertTrue(responseEntity instanceof Map<?, ?>);
            Map<?, ?> responseBody = (Map<?, ?>) responseEntity;
            Object userEntity = responseBody.get("user");
            assertTrue(userEntity instanceof Map<?, ?>);
            Map<?, ?> user = (Map<?, ?>) userEntity;
            assertEquals("&lt;b&gt;Workshop &amp; Student&lt;/b&gt;", user.get("displayName"));
        }
    }

    private static void assertLoginError(Map<String, String> requestBody, int status, String message) {
        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/auth/login")
                .then()
                .statusCode(status)
                .body("message", is(message));
    }
}
