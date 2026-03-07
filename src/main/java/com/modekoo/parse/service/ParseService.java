package com.modekoo.parse.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.modekoo.parse.config.ConfigBean;
import com.modekoo.parse.enums.FieldType;
import com.modekoo.parse.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ParseService {
    private final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();
    private final String[] passStr = {"desc", "type", "length", "old"};
    private final ResourceLoader resourceLoader;

    private final ConfigBean config;
    public ParseService(ConfigBean config, ResourceLoader resourceLoader) {
        this.config = config;
        this.resourceLoader = resourceLoader;
    }

    public String jsonToFlat(Map<String, Object> dataMap, String fileName, String charset){
        log.debug("ParseServce > jsonToFlat");
        StringBuilder sb = new StringBuilder();

        Resource resource = resourceLoader.getResource(
                config.getJsonTemplatePath() + fileName + ".json"
        );

        //Resource resource = new ClassPathResource("json/" + fileName + ".json");

        try {
            JsonReader jr = new JsonReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            JsonObject jo = gson.fromJson(jr, JsonObject.class);
            jr.close();

            Map<String, Object> structureMap = gson.fromJson(jo, LinkedTreeMap.class);
            jsonToFlatSelf(structureMap, dataMap, charset, sb);

        }catch (Exception e){
            log.error("jsonToFlat failed. fileName={}, charset={}", fileName, charset, e);
            throw new RuntimeException(e);  //커스텀 에러로 던질것
        }
        return sb.toString();
    }

    private void jsonToFlatSelf(Map<String, Object> structureMap, Map<String, Object> dataMap, String charset, StringBuilder sb){
        if(structureMap == null || structureMap.isEmpty()){
            return;
        }

        for(String key : structureMap.keySet()){
            if(Arrays.stream(passStr).filter(ignoreKey -> ignoreKey.equals(key)).count() > 0)
                continue;

            log.debug("key = {}", key);
            Map<String, Object> underMap = new LinkedTreeMap<>();
            if(structureMap.containsKey("type")
                    && (String.valueOf(structureMap.get("type")).equals(FieldType.OBJECT.getCode())
                    || String.valueOf(structureMap.get("type")).equals(FieldType.LIST.getCode()))){
                underMap = (Map<String, Object>) structureMap.get(key);
            }

            //O : 오브젝트
            if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.OBJECT.getCode())){
                //실제 데이터에 키가 없을 경우 임시키 생성 하여 계속 진행
                if(!dataMap.containsKey(key)){
                    Map<String, Object> tempMap = new LinkedTreeMap<>();
                    dataMap.put(key, tempMap);
                }
                jsonToFlatSelf((Map<String, Object>) structureMap.get(key), (Map<String, Object>) dataMap.get(key), charset, sb);
            }
            //L : 리스트
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.LIST.getCode())){
                if(dataMap.containsKey(key)) {
                    List<Map> dataList = (List<Map>) dataMap.get(key);
                    if(dataList == null || dataList.size() < 1)
                        continue;

                    for(int i=0; i<dataList.size(); i++){
                        jsonToFlatSelf(underMap, dataList.get(i), charset, sb);
                    }
                }

            }
            //A : 원시배열
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.ARRAY.getCode())){
                String underKey = getUnderKey(underMap);
                Map<String, Object> underUnderMap = (Map<String, Object>) underMap.get(underKey);
                String type = String.valueOf(underUnderMap.get("type"));
                int length = !underUnderMap.get("length").equals("") ?
                        Integer.parseInt(String.valueOf(underUnderMap.get("length"))) : 0;
                List<Object> dataList = (List<Object>) dataMap.get(key);
                String value = "";

                for(Object ojb : dataList){
                    value = Utils.cutStringToBytes(String.valueOf(ojb), length, charset);
                    value = Utils.stringPad(type, value, charset, length);
                    sb.append(value);
                }
            }
            //S, N 실제 데이터(String, Number)
            else if(underMap.containsKey("type")
                    && (String.valueOf(underMap.get("type")).equals(FieldType.STRING.getCode())
                    || String.valueOf(underMap.get("type")).equals(FieldType.NUMBER.getCode()))){
                String type = String.valueOf(underMap.get("type"));
                String value = dataMap.containsKey(key) ? String.valueOf(dataMap.get(key)) : "";
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
            //LN -> 배열(L)의 이름을 value로 받아 배열의 size길이로 사용
            //배열 이름이 dataMap에 없으면 0
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.LIST_COUNT.getCode())){
                String type = FieldType.NUMBER.getCode();
                String mapKey = String.valueOf(underMap.get("value"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                List<Map<String, Object>> listValue = dataMap.containsKey(mapKey) ?
                        (List<Map<String, Object>>) dataMap.get(mapKey) : null;
                String value = String.valueOf(listValue != null && listValue.size()>0 ? listValue.size() : 0);
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.JSON_STRING.getCode())){
                String type = String.valueOf(underMap.get("type"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                String value = gson.toJson(dataMap.get(key));
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
        }
    }

    public Map<String, Object> flatToJson(String flatStr, String fileName, String charset){

        log.debug("ParseService > flatToJson");
        Map<String, Object> resultMap = new LinkedTreeMap<>();

        Resource resource = resourceLoader.getResource(
                config.getJsonTemplatePath() + fileName + ".json"
        );

        try {
            JsonReader jr = new JsonReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            JsonObject jo = gson.fromJson(jr, JsonObject.class);
            jr.close();
            Map<String, Object> structureMap = gson.fromJson(jo, LinkedTreeMap.class);

            flatToJsonSelf(structureMap, flatStr, charset, resultMap);
        }
        catch (Exception e){
            log.error("jsonToFlat failed. fileName={}, charset={}", fileName, charset, e);
            throw new RuntimeException(e);  //커스텀 에러로 던질것
        }
        return resultMap;
    }

    private String flatToJsonSelf(Map<String, Object> structureMap, String flatStr, String charset, Map<String, Object> resultMap){
        if(structureMap == null || structureMap.isEmpty()){
            return "";
        }

        for(String key : structureMap.keySet()) {
            if (Arrays.stream(passStr).filter(ignoreKey -> ignoreKey.equals(key)).count() > 0)
                continue;

            Map<String, Object> underMap = new LinkedTreeMap<>();
            Map<String, Object> tempMap = new LinkedTreeMap<>();
            resultMap.put(key, tempMap);

            if(structureMap.containsKey("type")
                    && (String.valueOf(structureMap.get("type")).equals(FieldType.OBJECT.getCode())
                    || String.valueOf(structureMap.get("type")).equals(FieldType.LIST.getCode())))
                underMap = (Map<String, Object>) structureMap.get(key);

            if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.OBJECT.getCode())){
                flatStr = flatToJsonSelf((Map<String, Object>) structureMap.get(key), flatStr, charset, tempMap);
            }
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.LIST.getCode())){
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                List<Map<String, Object>> list = new ArrayList<>();
                Map<String, Object> listTempMap;
                for(int i=0; i<length; i++){
                    listTempMap = new LinkedTreeMap<>();
                    flatStr = flatToJsonSelf(underMap, flatStr, charset, listTempMap);
                    list.add(listTempMap);
                }
                resultMap.put(key, list);
            }
            //원시배열의 경우
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.ARRAY.getCode())){
                String underKey = getUnderKey(underMap);
                Map<String, Object> underUnderMap = (Map<String, Object>) underMap.get(underKey);
                String type = String.valueOf(underUnderMap.get("type"));
                int size = Integer.parseInt(String.valueOf(underMap.get("length")));
                int length = Integer.parseInt(String.valueOf(underUnderMap.get("length")));

                List<Object> list = new ArrayList<>();
                for(int i=0; i<size; i++){
                    byte[] flatStrBtyes = flatStr.getBytes(Charset.forName(charset));
                    String value = new String(flatStrBtyes, 0, length, Charset.forName(charset));
                    flatStr = new String(Arrays.copyOfRange(flatStrBtyes, length, flatStrBtyes.length), Charset.forName(charset) );
                    value = Utils.stringReduce(type, value);
                    if(type.equals(FieldType.NUMBER.getCode()))
                        list.add(Integer.parseInt(value));
                    else
                        list.add(value);
                }
                resultMap.put(key, list);
            }
            else if(underMap.containsKey("type")
                    && (String.valueOf(underMap.get("type")).equals(FieldType.STRING.getCode())
                    || String.valueOf(underMap.get("type")).equals(FieldType.NUMBER.getCode()))){
                String type = String.valueOf(underMap.get("type"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                byte[] flatStrBytes = flatStr.getBytes(Charset.forName(charset));

                String value = new String(flatStrBytes, 0, length, Charset.forName(charset));
                flatStr = new String(Arrays.copyOfRange(flatStrBytes, length, flatStrBytes.length));
                value = Utils.stringReduce(type, value);
                resultMap.put(key, value);

            }
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.LIST_COUNT.getCode())){
                String type = FieldType.NUMBER.getCode();
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                byte[] flatStrBytes = flatStr.getBytes(Charset.forName(charset));

                String value = new String(flatStrBytes, 0, length, Charset.forName(charset));
                flatStr = new String(Arrays.copyOfRange(flatStrBytes, length, flatStrBytes.length), Charset.forName(charset));
                value = Utils.stringReduce(type, value);

                String keyName = String.valueOf(underMap.get("value"));
                Map<String, Object> listMap = (Map<String, Object>) structureMap.get(keyName);
                listMap.put("length", value);
                resultMap.put("length", value);
            }
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals(FieldType.JSON_OBJECT.getCode())){
                String type = "S";
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                byte[] flatStrBytes = flatStr.getBytes(Charset.forName(charset));

                String value = new String(flatStrBytes, 0, length, Charset.forName(charset));
                flatStr = new String(Arrays.copyOfRange(flatStrBytes, length, flatStrBytes.length), Charset.forName(charset));
                value = Utils.stringReduce(type, value);

                Map<String, Object> valueMap = gson.fromJson(value, Map.class);
                resultMap.put(key, valueMap);

            }
        }
        return flatStr;
    }

    private String getUnderKey(Map<String, Object> underMap){
        return underMap.keySet().stream()
                .filter(underKey -> Arrays.stream(passStr).noneMatch(ignoreKey -> ignoreKey.equals(underKey)))
                .findFirst()
                .orElse("");
    }
}
