package com.modekoo.parse.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.modekoo.parse.config.ConfigBean;
import com.modekoo.parse.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ParseService {
    private final String[] passStr = {"desc", "type", "length", "old"};
    private final ConfigBean config;
    public ParseService(ConfigBean config) {this.config = config;}

    public String jsonToFlat(Map<String, Object> dataMap, String fileName, String charset){
        log.debug("ParseServce > jsonToFlat");
        StringBuilder sb = new StringBuilder();

        try {
            JsonReader jr = new JsonReader(new FileReader(config.getJsonTemplatePath() + "/" + fileName + ".json"));
            JsonObject jo = new Gson().fromJson(jr, JsonObject.class);
            jr.close();

            Map<String, Object> structureMap = new Gson().fromJson(jo, LinkedTreeMap.class);
            jsonToFlatSelf(structureMap, dataMap, charset, sb);

        }catch (Exception e){

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
            if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("O")
                || String.valueOf(structureMap.get("type")).equals("L")){
                underMap = (Map<String, Object>) structureMap.get(key);
            }

            //O : 오브젝트
            if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("O")){
                //실제 데이터에 키가 없을 경우 임시키 생성 하여 계속 진행
                if(!dataMap.containsKey(key)){
                    Map<String, Object> tempMap = new LinkedTreeMap<>();
                    dataMap.put(key, tempMap);
                }
                jsonToFlatSelf((Map<String, Object>) structureMap.get(key), (Map<String, Object>) dataMap.get(key), charset, sb);
            }
            //L : 리스트
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("L")){
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
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("A")){
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
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("S")
                    || String.valueOf(underMap.get("type")).equals("N")){
                String type = String.valueOf(underMap.get("type"));
                String value = dataMap.containsValue(key) ? String.valueOf(dataMap.get(key)) : "";
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
            //LN -> 배열(L)의 이름을 value로 받아 배열의 size길이로 사용
            //배열 이름이 dataMap에 없으면 0
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("LN")){
                String type = "N";
                String mapKey = String.valueOf(underMap.get("value"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                List<Map<String, Object>> listValue = dataMap.containsValue(mapKey) ?
                        (List<Map<String, Object>>) dataMap.get(mapKey) : null;
                String value = String.valueOf(listValue != null && listValue.size()>0 ? listValue.size() : 0);
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
            else if(underMap.containsKey("type") && String.valueOf(underMap.get("type")).equals("JS")){
                String type = String.valueOf(underMap.get("type"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                String value = new Gson().toJson(dataMap.get(key));
                value = Utils.cutStringToBytes(value, length, charset);
                value = Utils.stringPad(type, value, charset, length);
                sb.append(value);
            }
        }
    }

    public Map<String, Object> flatToJson(String flatStr, String fileName, String charset){

        log.debug("ParseService > flatToJson");
        Map<String, Object> resultMap = new LinkedTreeMap<>();

        try{
            JsonReader jr = new JsonReader(new FileReader(config.getJsonTemplatePath() + "/" + fileName + ".json"));
            JsonObject jo = new Gson().fromJson(jr, JsonObject.class);
            jr.close();
            Map<String, Object> structureMap = new Gson().fromJson(jo, LinkedTreeMap.class);

            flatToJsonSelf(structureMap, flatStr, charset, resultMap);
        }
        catch (Exception e){

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

            if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("O")
                || String.valueOf(structureMap.get("type")).equals("L"))
                underMap = (Map<String, Object>) structureMap.get(key);

            if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("O")){
                flatToJsonSelf((Map<String, Object>) structureMap.get(key), flatStr, charset, tempMap);
            }
            else if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("L")){
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
            else if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("A")){
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
                    if(type.equals("N"))
                        list.add(Integer.parseInt(value));
                    else
                        list.add(value);
                }
                resultMap.put(key, list);
            }
            else if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("S")
                    || String.valueOf(structureMap.get("type")).equals("N")){
                String type = String.valueOf(underMap.get("type"));
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                byte[] flatStrBytes = flatStr.getBytes(Charset.forName(charset));

                String value = new String(flatStrBytes, 0, length, Charset.forName(charset));
                flatStr = new String(Arrays.copyOfRange(flatStrBytes, length, flatStrBytes.length));
                value = Utils.stringReduce(type, value);
                resultMap.put(key, value);

            }
            else if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("LN")){
                String type = "N";
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
            else if(structureMap.containsKey("type") && String.valueOf(structureMap.get("type")).equals("JO")){
                String type = "S";
                int length = !String.valueOf(underMap.get("length")).equals("") ?
                        Integer.parseInt(String.valueOf(underMap.get("length"))) : 0;
                byte[] flatStrBytes = flatStr.getBytes(Charset.forName(charset));

                String value = new String(flatStrBytes, 0, length, Charset.forName(charset));
                flatStr = new String(Arrays.copyOfRange(flatStrBytes, length, flatStrBytes.length), Charset.forName(charset));
                value = Utils.stringReduce(type, value);

                Map<String, Object> valueMap = new Gson().fromJson(value, Map.class);
                resultMap.put(key, valueMap);

            }
        }
        return flatStr;
    }

    private String getUnderKey(Map<String, Object> underMap){
        String realKey = "";

        try{
            realKey = underMap.keySet().stream().filter(
                    underKey -> Arrays.stream(passStr).filter(
                            ignoreKey -> !ignoreKey.equals(underKey)).count() > 0)
                    .findFirst().get();
        }catch (Exception e){

        }

        return realKey;
    }
}
