package com.modekoo.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.modekoo.parse.service.ParseService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
class ParseApplicationTests {

    @Autowired
    private ParseService parseService;

    private Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    @Test
    void jsonToFlat_sample(){
        Map<String, Object> sampleMap = new HashMap<>();
        sampleMap.put("name", "modekoo");
        sampleMap.put("age", 38);

        String fixedLengthSTR = parseService.jsonToFlat(sampleMap, "sample", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "sample", "UTF-8");
        log.debug(jsonMap.toString());

        assertEquals("modekoo   038", fixedLengthSTR);

    }

    @Test
    void jsonToFlat_bigDepth() {
        String data = """
				{
				  "a": {
				    "b": {
				      "c": {
				        "d": "HELLO",
				        "e": 12
				      },
				      "f": "ABCD"
				    },
				    "g": "ZZ"
				  },
				  "h": "END"
				}
				""";

        Map<String, Object> dataMap = gson.fromJson(data, Map.class);
        log.debug("dataMap = {}", dataMap);

        String fixedLengthSTR = parseService.jsonToFlat(dataMap, "bigDepth", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "bigDepth", "UTF-8");
        log.debug(jsonMap.toString());

        Map<String, Object> a = (Map<String, Object>) jsonMap.get("a");
        Map<String, Object> b = (Map<String, Object>) a.get("b");
        Map<String, Object> c = (Map<String, Object>) b.get("c");

        assertEquals("HELLO", c.get("d"));
        assertEquals("12", String.valueOf(c.get("e")));
        assertEquals("ABCD", b.get("f"));
        assertEquals("ZZ", a.get("g"));
        assertEquals("END", jsonMap.get("h"));

        assertEquals("HELLO012ABCDZZEND", fixedLengthSTR);

    }

    @Test
    void jsonToFlat_missingField(){
        String data = """
				{
				  "user": {
				    "name": "KOO"
				  },
				  "status": "Y"
				}
				""";

        Map<String, Object> dataMap = gson.fromJson(data, Map.class);
        log.debug("dataMap = {}", dataMap);

        String fixedLengthSTR = parseService.jsonToFlat(dataMap, "missing", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "missing", "UTF-8");
        log.debug(jsonMap.toString());

        assertEquals("KOO                Y", fixedLengthSTR);
    }


    @Test
    void jsonToFlat_list(){
        String data = """
				{
				  "items": [
				    {
				      "code": "A001",
				      "qty": 3
				    },
				    {
				      "code": "B002",
				      "qty": 25
				    }
				  ]
				}
				""";

        Map<String, Object> dataMap = gson.fromJson(data, Map.class);
        log.debug("dataMap = {}", dataMap);

        String fixedLengthSTR = parseService.jsonToFlat(dataMap, "list", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "list", "UTF-8");
        log.debug(jsonMap.toString());

        assertEquals("A001003B002025", fixedLengthSTR);
    }

    @Test
    void jsonToFlat_array(){
        String data = """
					{
					  "tags": ["RED", "BLUE", "GREEN"]
					}
				""";

        Map<String, Object> dataMap = gson.fromJson(data, Map.class);
        log.debug("dataMap = {}", dataMap);

        String fixedLengthSTR = parseService.jsonToFlat(dataMap, "array", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "array", "UTF-8");
        log.debug(jsonMap.toString());

        assertEquals("RED  BLUE GREEN", fixedLengthSTR);
    }

    @Test
    void jsonToFlat_listCount(){
        String data = """
					{
				   "items": [
				     {
				       "name": "AA"
				     },
				     {
				       "name": "BB"
				     }
				   ]
				 }
				""";

        Map<String, Object> dataMap = gson.fromJson(data, Map.class);
        log.debug("dataMap = {}", dataMap);

        String fixedLengthSTR = parseService.jsonToFlat(dataMap, "LNList", "UTF-8");
        log.debug("transfer = {}", fixedLengthSTR);

        Map<String, Object> jsonMap = parseService.flatToJson(fixedLengthSTR, "LNList", "UTF-8");
        log.debug(jsonMap.toString());

        assertEquals("002AA  BB  ", fixedLengthSTR);
    }
}
