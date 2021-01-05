package test;

import org.json.JSONObject;

public class JsonTest {
    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        String string = "abc\nefg";
        jsonObject.put("string", string);
        String JsonString = jsonObject.toString();
        JSONObject jsonObject1 = new JSONObject(JsonString);
        System.out.println(jsonObject1.get("string"));
    }
}
