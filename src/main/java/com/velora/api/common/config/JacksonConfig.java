package com.velora.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An ObjectMapper the application can inject directly.
 *
 * <p>Spring Boot configures JSON for HTTP messages without necessarily exposing a
 * bean, so anything that serializes on its own — the idempotency store replaying a
 * response, for one — has nothing to autowire.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // OffsetDateTime is everywhere in this domain; without this module it fails
        // to serialize at all.
        mapper.registerModule(new JavaTimeModule());

        // Dates as ISO-8601 strings, not epoch numbers — a stored response has to
        // stay readable and has to deserialize back into the same record.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // A replayed response was serialized by an older build. Unknown fields must
        // not break it.
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature
                        .FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}