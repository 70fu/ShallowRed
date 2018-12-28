package at.pwd.shallowred.tests;

import at.pwd.shallowred.ShallowRed;
import com.eclipsesource.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShallowRedSerializationTest
{
    //content is in resources/ShallowRedConfig
    private static final String json = "{\n" +
            "  \"selector\":{\"type\":\"random\"},\n" +
            "  \"expand\":[\n" +
            "    {\n" +
            "      \"id\":0,\n" +
            "      \"weight\":0.1\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\":1,\n" +
            "      \"weight\":0.2\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\":3,\n" +
            "      \"weight\":0.3\n" +
            "    }\n" +
            "  ],\n" +
            "  \"simulation\":[\n" +
            "    {\n" +
            "      \"id\":0,\n" +
            "      \"weight\":0.8\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\":5,\n" +
            "      \"weight\":0.6\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    void sameJSON()
    {
        ShallowRed agent = new ShallowRed(json);
        String serialized = agent.toJSONConfig();

        assertEquals(Json.parse(json),Json.parse(serialized));
    }
}