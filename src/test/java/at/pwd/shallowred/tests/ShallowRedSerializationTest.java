package at.pwd.shallowred.tests;

import at.pwd.shallowred.ShallowRed;
import com.eclipsesource.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShallowRedSerializationTest
{
    //content is in resources/ShallowRedConfig
    private static final String json = "{\n" +
            "  \"C\":0.7071067811865475,\n" +
            "  \"minmaxInfluence\":0,\n" +
            "  \"selector\":{\"type\":\"roulette\"},\n" +
            "  \"expand\": {\n" +
            "    \"0\": {\n" +
            "      \"weight\": 0.5\n" +
            "    },\n" +
            "    \"1\": {\n" +
            "      \"weight\": 1\n" +
            "    },\n" +
            "    \"2\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"3\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"4\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"5\": {\n" +
            "      \"weight\": 0.75\n" +
            "    }\n" +
            "  },\n" +
            "  \"simulation\":{\n" +
            "    \"0\": {\n" +
            "      \"weight\": 0.5\n" +
            "    },\n" +
            "    \"1\": {\n" +
            "      \"weight\": 1\n" +
            "    },\n" +
            "    \"2\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"3\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"4\": {\n" +
            "      \"weight\": 0.05\n" +
            "    },\n" +
            "    \"5\": {\n" +
            "      \"weight\": 0.75\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    void sameJSON()
    {
        ShallowRed agent = new ShallowRed(json);
        String serialized = agent.toJSONConfig();

        assertEquals(Json.parse(json),Json.parse(serialized));
    }
}