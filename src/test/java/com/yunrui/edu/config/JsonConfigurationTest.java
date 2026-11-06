package com.yunrui.edu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonConfigurationTest {
    @Test
    void serializesLongIdsAsStringsSoBrowsersDoNotRoundThem() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonConfiguration().longIdAsStringModule());

        String json = mapper.writeValueAsString(new IdResponse(365108083858345984L));

        assertThat(json).isEqualTo("{\"id\":\"365108083858345984\"}");
    }

    private record IdResponse(long id) {
    }
}
