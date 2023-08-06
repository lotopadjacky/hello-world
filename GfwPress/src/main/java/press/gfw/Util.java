package press.gfw;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Util {
    public static int getIdlePort() {
    	
   	   	
    	int startPort = Constant.PORT_START;
    	int endPort = Constant.PORT_END;
        
    	int port = startPort;
    	
    	Set<Integer> usedPorts = LinksTab.getUsedPorts();
    	
        while(port <= endPort) {
    		
        	if(!usedPorts.contains(port)) {
    		     break;  //找到空余端口
    		}
    		port +=1;
    	}
    	
        if(port > endPort) {
        	return -1;  //没找到空余端口
        }
        
        return port;

    }
	
    /*
	 * 生成随机密码
	 * 1、长度10个字符 2、至少包含一个数字 3、至少包含一个大写字母 4、至少包含一个小写字母 5、不得包含空格
	 */
	public static String generatePassword() {
		char[] str = new char[10];
		
		//特殊字符范围
		char[] specialchar = {'!','@','#','$','%','&','*','(',')','-','=','+','.',',',':',';','<','>'}; 
		
		//必须包含4类字符
		str[0] = (char)('a' + (int)(Math.random()*26));
		str[1] = (char)('A' + (int)(Math.random()*26));
		str[2] = (char)('0' + (int)(Math.random()*10));
		str[3] = specialchar[(int)(Math.random()*18)];
		
		//随机产生其他字符
		for(int i=4;i<=9;i++) {
			int j = (int)(Math.random()*80);
		    if(j<=26) 
		    	str[i] = (char)('a' + (int)(Math.random()*26));
		    else if(j<=52)
		    	str[i]=(char)('A' + (int)(Math.random()*26));
		    else if(j<=62)
		    	str[i]=(char)('0' + (int)(Math.random()*26));
		    else 
		    	str[i]=specialchar[j-63];
		}
		
		//乱序
		StringBuilder sb=new StringBuilder();
		sb.append(str);
		for(int i=10;i>0;i--) {

			int j = (int)(Math.random()*i);
			sb.getChars(j, j+1, str, 10-i);
			sb.deleteCharAt(j);
		}
		
		return sb.toString();
	}
	
	/**
	 * 打印信息
	 *
	 * @param str
	 */
	public static void log(String str) {

		String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);

		System.out.println("[" + time + "] " + str);

	}
	
	/*
	 * JSON处理: 字符串转json对象
	 * 
	 */
	
	public static JSONObject getJSON(String data) {

		if (data == null || (data = data.trim()).length() == 0) {

			return null;

		}

		JSONParser p = new JSONParser();

		try {

			return (JSONObject) p.parse(data);

		} catch (ParseException ex) {

			log("分析JSON字符串时出错：");

			ex.printStackTrace();

		}

		return null;

	}
	
	public static Date str2Date(String str,String format) {
		DateFormat sdf = new SimpleDateFormat(format);
		Date mydate = null;
		try {
		
			mydate = sdf.parse(str);
		
		} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
  	 
	    	mydate = null;
		}
		return mydate;
	}
	
	public static String date2String(Date mydate,String format) {
		DateFormat sdf = new SimpleDateFormat(format);
		return(sdf.format(mydate));
	}
}
