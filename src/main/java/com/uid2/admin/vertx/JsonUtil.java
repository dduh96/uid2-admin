package com.uid2.admin.vertx;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    public static ObjectWriter createJsonWriter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        return mapper.writer(pp);
    }
}
