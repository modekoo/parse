package com.modekoo.parse.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Utils {
    public static String cutStringToBytes(String str, int cutByteLen, String charset){
        if(cutByteLen < 1) return "";

        StringBuilder sb = new StringBuilder(cutByteLen);

        try {
            if (str.getBytes(charset).length > cutByteLen) {
                int nCnt = 0;
                for(char ch : str.toCharArray()) {
                    nCnt += String.valueOf(ch).getBytes(charset).length;
                    if(nCnt > cutByteLen)
                        break;
                    sb.append(ch);
                }
            }
            else
                return str;
        } catch (Exception e){

        }
        return sb.toString();
    }

    public static String stringPad(String type, String data, String charset, int maxByteLen){

        StringBuilder sb = new StringBuilder();

        try{
            int curByteLen = data.getBytes(charset).length;
            int fillLen = maxByteLen - curByteLen;
            String padStr;

            if(fillLen > 0){
                if("N".equals(type)){
                    padStr = String.format("%" + fillLen + "s", "").replaceAll(" ", "0");
                    sb.append(data);
                    sb.append(padStr);
                }
                //type : S, JS, default
                else{
                    padStr = String.format("%-" + fillLen + "s", "");
                    sb.append(data);
                    sb.append(padStr);
                }
            }
            else
                sb.append(data);

        }
        catch (Exception e){

        }
        return sb.toString();
    }

    public static String stringReduce(String type, String data){
        if(type.equals("N")){
            if(String.valueOf(data).equals("")){
                return "0";
            }
            else{
                data = new DecimalFormat("0").format(new BigDecimal(data.trim()));
            }
        }
        else if(type.equals("S")){
            data = data.trim();
        }
        return data;
    }

}
