/**
*
*    GFW.Press
*    Copyright (C) 2016  chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
**/
package press.gfw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Hashtable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * GFW.Press配置文件管理
 *
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 *
 */
public class Config {

	public static final String CHARSET = "utf-8";

	public static void main(String[] args) {

		Config c = new Config();

		c.getUser(); //测试?

		//System.out.println(LinksTab);

	}

	private File clientFile = null;

	private File serverFile = null;

	private File userFile = null;

	private long userFileTime = 0L;

	public Config() {

		clientFile = new File("client.json");

		serverFile = new File("server.json");

		userFile = new File("user.txt");

	}

	/**
	 * 获取客户端配置文件
	 *
	 * @return
	 */
	public JSONObject getClientConfig() {

		return getJSON(read(clientFile));

	}

	/**
	 * 字符串转JSON对象
	 *
	 * @param data
	 * @return
	 */
	public JSONObject getJSON(String data) {

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

	/**
	 * 获取服务器配置
	 *
	 * @return
	 */
	public JSONObject getServerConfig() {

		return getJSON(read(serverFile));

	}
    
	/*
	 * 动态获取用户port与key
	 * 缺少异常处理
	 *///
	public Hashtable<String, String> getUser() {
		
		
		HashMap<String,LinkItem> tempLinksTab = LinksTab.getAllLinks();
		
		Hashtable<String,String> users = new Hashtable<String,String>(tempLinksTab.size());
         
		for(String cid : tempLinksTab.keySet()){
			users.put(cid,tempLinksTab.get(cid).getPassword());
		}
		
		return users;
			
	}
	
	///同步全局表，同时返回临时表
	public boolean initLinksTab() {
		if (userFile.lastModified() == userFileTime) { ///=0L表示文件不存在

			return false;

		}

		userFileTime = userFile.lastModified();

		String text = read(userFile);

		if (text == null) {///文件为空

			return false;

		}

		String[] lines = text.trim().split("\n");

		text = null;

		if (lines == null || lines.length == 0) {

			return false;

		}
        
		HashMap<String, LinkItem> links = new HashMap<String,LinkItem>(lines.length);
		
        /*链路文件格式:23-08-04
		*23072500001 10001 m3ihEhFziT 230801 1000 100 200 1 230701 23060212304512  
		*customID/port/password/enableDate/enableMBytes/usedUpMBytes/usedDownMBytes/linktype/stopDate/lastAddFeeID
		*/
		for (String line : lines) {
			
			String[] cols = line.trim().split(" ");

 			//此处需要判断格式是否正确，不正确退出，待处理
//			if (cols == null || cols.length < 8 || !(cols[0] = cols[0].trim()).matches("\\d+")
//					|| (cols[cols.length - 1] = cols[cols.length - 1].trim()).length() < 8) {
//
//				continue;
//
//			}
			
            LinkItem linkItem =  new LinkItem();
            
            linkItem.setCustomID(cols[0]);
            linkItem.setPort(Integer.parseInt(cols[1]));
            linkItem.setPassword(cols[2]);
            linkItem.setEnableDate(Util.str2Date(cols[3],"yyMMdd"));
            linkItem.setEnableMBytes(Integer.parseInt(cols[4]));
            linkItem.setFeeType(Integer.parseInt(cols[5]));
            linkItem.setUsedUpMBytes(Integer.parseInt(cols[6]));
            linkItem.setUsedDownMBytes(Integer.parseInt(cols[7]));
            linkItem.setLastAddFeeID(cols[8]);
            linkItem.setStopDate(Util.str2Date(cols[9],"yyMMddHHmm"));

            
			links.put(cols[0], linkItem);
		}
       
		LinksTab.setLinksTab(links);
	
		return  true;

	}

	/**
	 * 打印信息
	 *
	 * @param o
	 */
	private void log(Object o) {

		String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);

		System.out.println("[" + time + "] " + o.toString());

	}

	/**
	 * 读文件内容
	 *
	 * @param file
	 * @return
	 */
	public String read(File file) {

		int size = 0;

		if (file == null || !file.exists() || (size = (int) file.length()) == 0) {

			return null;

		}

		byte[] bytes = new byte[size];

		FileInputStream fis = null;

		int count = 0;

		try {

			fis = new FileInputStream(file);

			for (; size != count;) {

				int read = fis.read(bytes, count, size - count);

				if (read == -1) {

					break;

				}

				count += read;

			}

		} catch (IOException ex) {

			log("读文件出错：");

			ex.printStackTrace();

			return null;

		} finally {

			try {

				fis.close();

			} catch (IOException ex) {

			}

		}

		if (count != size) {

			return null;

		}

		try {

			return new String(bytes, CHARSET);

		} catch (UnsupportedEncodingException ex) {

			log("获取文件内容出错：");

			ex.printStackTrace();

		}

		return null;

	}

	/**
	 * 保存内容到文件
	 *
	 * @param file
	 * @param text
	 * @return
	 */
	public boolean save(File file, String text) {

		if (file == null || text == null || (text = text.trim()).length() == 0) {

			return false;

		}

		FileOutputStream fos = null;

		try {

			fos = new FileOutputStream(file);

			fos.write(text.getBytes(CHARSET));

			fos.flush();

		} catch (IOException ex) {

			log("写文件出错：");

			ex.printStackTrace();

			return false;

		} finally {

			try {

				fos.close();

			} catch (IOException ex) {

			}

		}

		return true;

	}

	/**
	 * 保存客户端配置文件
	 *
	 * @param json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean saveClientConfig(JSONObject json) {

		if (json == null) {

			return false;

		}

		JSONObject _json = getClientConfig();

		if (_json == null) {

			_json = json;

		} else {

			_json.putAll(json);

		}

		return save(clientFile, _json.toJSONString());

	}

	/**
	 * 保存服务器配置文件
	 *
	 * @param json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean saveServerConfig(JSONObject json) {

		if (json == null) {

			return false;

		}

		JSONObject _json = getServerConfig();

		if (_json == null) {

			_json = json;

		} else {

			_json.putAll(json);

		}

		return save(serverFile, _json.toJSONString());

	}

}
