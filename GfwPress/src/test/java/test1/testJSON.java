package test1;

import java.util.List;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import javax.json.JsonObject;
import com.alibaba.fastjson.*;

public class testJSON {
   
	public static void main(String[] args) {

		JSONObject json =new JSONObject();
		String s ="zhang";
		int i=100;
		json.put("id", i);
		json.put("name",s);
		json.put("old","20");
	    s = json.toString();
		System.out.println(s);
		s= json.toJSONString();
		System.out.println(s);

		String str = "{'array':[{'id':5,'name':'张三'},{'id':6,'name':'李四'}]}";
		String arraystr = "[{'id':5,'name':'张三'},{'id':6,'name':'李四'}]";
		
		//JSONArray jsonArray = new JSONArray(arraystr);
		//JSONObject jsonobject = JSONObject(str);
		
		String jsonString = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
		JSONObject json1 = JSONObject.parseObject(jsonString);
		
		JSONArray jarr = JSONArray.parseArray(arraystr);
		System.out.println(jarr.getJSONObject(0).get("id"));
		System.out.println(jarr.getJSONObject(1).get("id"));
		
		// 获取数组
		//jsonobject = 
		//jsonArray = jsonobject.getJSONArray("array");
		//System.out.println(jsonArray.getJSONObject(0).get("name"));
		
	}

}
