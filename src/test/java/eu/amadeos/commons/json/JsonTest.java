package eu.amadeos.commons.json;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test
    void parsesNestedValues() {
        Object o = Json.parse("{\"a\": [1, 2.5, \"x\\\"y\\u00e4\"], \"b\": {\"c\": true, \"d\": null}}");
        assertEquals(List.of(1.0, 2.5, "x\"yä"), Json.member(o, "a"));
        assertEquals(Boolean.TRUE, Json.member(Json.member(o, "b"), "c"));
        assertEquals("1", Json.str(Map.of("n", 1.0), "n"));
        assertEquals(42, Json.integer(Map.of("n", "42"), "n"));
        assertEquals(List.of(), Json.list(o, "missing"));
    }

    // --8<-- [start:round-trip]
    @Test
    void writesWhatItReads() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "Tab\tand \"quote\"");
        m.put("n", 128);
        m.put("list", List.of("a", 1.5, false));
        m.put("empty", List.of());
        String json = Json.write(m);
        assertEquals("Tab\tand \"quote\"", Json.str(Json.parse(json), "name"));
        assertEquals(128, Json.integer(Json.parse(json), "n"));
        assertEquals(List.of("a", 1.5, false), Json.member(Json.parse(json), "list"));
    }
    // --8<-- [end:round-trip]

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\": }"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseArray("{}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1] x"));
    }
}
