package press.gfw;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Date;

import org.json.simple.JSONObject;

/*
 * 与充值服务器cs的通讯
 * 1. 建立监听进程， ls(Server) cs(Client)
 * 2. 增加新链路:   cs->ls
 * 3. 删除链路:    cs->ls
 * 4. 增加上网流量:  cs->ls
 * 5. 获取用户流量:  cs->ls
 */
public class ChargeThread extends Thread {
    private final String  ADDCUSTOM_REQUEST = "101";
    
    private final String  DELCUSTOM_REQUEST = "109";

    private final String  ADDFEE_REQUEST = "102";
    
    private final String  QUERYCUSTOM_REQUEST = "103";
    
	private ServerSocket chargeServerSocket = null;

	private Socket chargeSocket = null;

	private InputStream inputStream = null;

	private OutputStream outputStream = null;
	
	private boolean endSocket = false;

	public ChargeThread(ServerSocket chargeServerSocket) {

		this.chargeServerSocket = chargeServerSocket;

	}

	private void _sleep(long ms) {
		try {

			sleep(ms);

		} catch (InterruptedException e) {

		}
	}

	/*
	 * 添加用户:为用户分配port及KEY,添加到链路表.此时链路还未建立，下次轮训后可上网 参数: 返回值: 0:成功 1:失败，已存在 2:失败，无空余端口
	 * 3:失败，其他错误
	 */
	private int addCustomInLinksTab(String customID,Date enableDate, int enableMBytes,int feeType,int usedUpMBytes,int usedDownMBytes) {

		if (LinksTab.getLink(customID) != null) { // 键值为 CustomID? port?
			return 1; // 已存在
		}
		
		String password = Util.generatePassword();

			LinkItem item = new LinkItem();
			item.setCustomID(customID);
			item.setPort(0); //设置为0,到linksTab的方法中去分配端口, 那里上锁
			item.setPassword(password);
			item.setEnableMBytes(enableMBytes);
			item.setEnableDate(enableDate);
			item.setFeeType(feeType);
			item.setUsedUpMBytes(usedUpMBytes);
			item.setUsedDownMBytes(usedDownMBytes);
			
			return (LinksTab.addLink(item));
	}

	/*
	 * 删除用户 返回值： 1.null:删除失败 2.linkItem:删除成功，则返回当前链路信息
	 */
	private LinkItem delCustomInLinksTab(String customID) {
		 
		return LinksTab.delLink(customID);
	}

	/*
	 * 为用户增加流量:增加上网流量 ,延长上网时间。延长时间可以按“天”或“月”
	 *
	 */
	private int addEnableMbytesOrDateInLinksTab(String orderID,String customID, int mBytes, int days, int feeType){
       
		return(LinksTab.addEnableMbytesOrDate(orderID,customID, mBytes, days,feeType));
	
	}

	/*
	 * 
	 */
	private void close() {

		close(inputStream);

		close(outputStream);

		close(chargeSocket);

		close(chargeServerSocket);

	}

	/*
	 * 关闭socket
	 */
	private void close(Closeable o) {

		if (o == null) {

			return;

		}

		try {

			o.close();

		} catch (IOException e) {

		}

	}

    /*
     * 与chargeServer一条连接,串行处理请求【以后并行?】
     * 1.接收一条请求
     * 2.处理
     * 3.应答
     * 4。接收下一条请求
     */
	@Override
	@SuppressWarnings("preview")
	public void run() {

		byte[] buffer = new byte[Constant.STR_BUFFER_MIN];//接收缓冲区
		byte[] read_bytes_buffer = new byte[Constant.STR_BUFFER_MAX];  //字符存放处，分析使用
		byte[] read_bytes = null;
		String questStr=null;		
        
		openSocket();
        
		while (!endSocket) { //等待charge请求:endSocket赋值可结束,何时赋值?
			
			int total_read_num = 0;
			int read_num = 0;
			
			//boolean needReopenSocket=false;
			
			while (total_read_num <= Constant.STR_BUFFER_MAX) { //寻找chargeserver的一条完整请求
                
				read_num = 0;
				read_bytes = null;
				questStr=null;
				
				
				
				try {
					read_num = inputStream.read(buffer);
				} catch (IOException ex) {  /// 异常：退出
					//needReopenSocket = true;
					break;
				}
				if (read_num == -1) { // -1表示流文件结束，退出
					break;
				}
                 
				System.arraycopy(buffer, 0, read_bytes_buffer, total_read_num-1, read_num);
	            total_read_num += read_num;
			    
	            read_bytes = new byte[total_read_num]; 
			    System.arraycopy(read_bytes_buffer, 0, read_bytes, 0, total_read_num);
			    
			    String str=new String(read_bytes,Charset.forName(Constant.CHARSET));//byte[]转为String,UTF-8规则
			   
			    
			    if(str.contains("}")) {
			    	
			    	int begin= str.indexOf('{');
				    int end = str.indexOf("}");
				    if(begin >=0 && begin < end ) {
				         questStr = questStr.substring(begin,end+1);
				    }
			    	break;   //发现通讯指令结束符,立即去分析执行指令
			    
			    }
			 }
						
			if(questStr != null ) { //执行chargeServer的请求,之后应答
				
				doChargeQuest(questStr,outputStream);
				
			}
			
			read_bytes = null; 
			questStr = null;
			
            //通讯异常, 重新启动socket?
			//if(needReopenSocket) {
			    //close();
				//openChargeSocket();      
			//}
		
		 }
		
		 close(); 
		 buffer = null;
		 read_bytes_buffer =null;
		 
	}
	
	private void  openSocket()		
	{
		try {

		chargeSocket = chargeServerSocket.accept();
		
		chargeSocket.setSoTimeout(Constant.SOCK_TIMEOUT);
		chargeSocket.setTcpNoDelay(true);
		// 打开 keep-alive
		chargeSocket.setKeepAlive(true);
		// 获取输入输出流
		inputStream = chargeSocket.getInputStream();
		outputStream = chargeSocket.getOutputStream();

	 } catch (IOException ex) {

		Util.log("连接计费服务器出错！");
		close();
		return;
	 }
  }
	
	private int doChargeQuest(String request,OutputStream outputStream) {
		
		JSONObject requestJson = Util.getJSON(request);
		JSONObject responseJson = null;
		
		if(requestJson == null) {
			return 1; //json解析出错，返回
		}
		
		String requestID = (String)requestJson.get("requestID".toString());
		
		
		if(requestID.equals(ADDCUSTOM_REQUEST )) {
			responseJson = addCustomRequest(requestJson);
		}
		
		if(requestID.equals(DELCUSTOM_REQUEST )) {
			  responseJson = delCustomRequest(requestJson);
		}
		
		if(requestID.equals(ADDFEE_REQUEST)) {
			  responseJson = addFeeRequest(requestJson);
		}
		
		if(requestID.equals(QUERYCUSTOM_REQUEST)) {
			  responseJson = queryCustomRequest(requestJson);
		}
		
		responseCharge(responseJson,outputStream);//应答

		return 0;
	}
	
	
    /* 
     * 新增用户:在LinksTab表增加客户链路,等Server进轮询LinksTab后启动通讯进程 
     * 场景说明: 1)注册后第一次充值 2)更换上网服务器。报文中key大小写无关,value大小写有关。 
	 * 请求报文:{"requestid":"101",        
	 *      "customID":"230802234400", //12位数字
	 *      "enableDate":"230801", //有效期
	 *      "enableMBytes":"2000", //可用流量(单位M)，分按天、周、月 等情况，看feeType
	 *      "feetype":"1" //流量包类型,用于计费,暂时都按“有效期内总流量”模式计费
	 *      "usedUpMBytes":"230831",//已用上传流量  已用流量适用转移线路用户，新用户为0 
	 *      "usedDownMBytes":"230831",//已用下载流量
	 *      }
	 * 应答报文:{"responseid":"101",
	 *      "customID":"2023080223440",
	 *      "code":"0" // 返回码: 0成功 1参数问题 2用户已存在 3没有空闲端口 4其他错误
	 *      "msg":{  //不成功返回"",成功返回如下参数
	 *             "port":"10021",    //链路端口
	 *             "key":"iasdudd34", //链路秘钥,加密?使用时解密?
	 *             }
	 *     }
    */ 
	private  JSONObject addCustomRequest(JSONObject requestJson) {

		JSONObject respJson = new JSONObject();
		
		String customID = (String)requestJson.get("customid".toString());
		Date enableDate = Util.str2Date((String)(requestJson.get("enabledate".toString())),"yyMMdd");
		int  enableMBytes = Integer.parseInt((String)requestJson.get("enablembytes".toString()));
		int  feeType = Integer.parseInt((String)requestJson.get("feetype".toString()));
		int  usedUpMBytes = Integer.parseInt((String)requestJson.get("usedupmbytes".toString()));
		int  usedDownMBytes = Integer.parseInt((String)requestJson.get("useddownmbytes".toString()));
		
		int r=addCustomInLinksTab(customID,enableDate,enableMBytes,feeType,usedUpMBytes,usedDownMBytes);

		String code = Integer.toString(r);
		respJson.put("responseid",ADDCUSTOM_REQUEST);
		respJson.put("customid", customID);
		respJson.put("code",code);
		
		if(r==0){ //成功
			LinkItem item = LinksTab.getLink(customID);
			JSONObject msgJson = new JSONObject();
			msgJson.put("port", Integer.toString(item.getPort()));
			msgJson.put("key", item.getPassword());//暂时未加密，升级处理
			
			respJson.put("msg", msgJson);
		} 
		else {
            
			respJson.put("msg","");
 			
		}

		return respJson;
	}
	/* *
	 * 删除用户:在LinksTab链路表删除该用户,等Server进轮询LinksTab后停止通讯进程。成功则返回被删用户完整信息. 
	 * 场景说明:更换线路时删除用户
	 * 请求报文:{"requestid":"109",
	 *        "costomID":"2023080223440"
	 *       }
	 * 应答报文:{"responseid":"109",
	 *        "customID":"2023080223440",
	 *        "code":"0",  //返回码:0成功 1用户不存在 2其他错误
	 *        msg:{ 
	 *              "port":"10021",
	 *              "key":* "iasdu**(加密)",
	 *              "enableDate":"230801",
	 *              "enableMBytes":"2000",
	 *              "feeType":"1",
	 *              "usedUpMBytes":"100",
	 *              "usedDownMbytes":"2000"
	 *              "stopDate":"230702"
	 *            }
	 *       } 
	 */
	private  JSONObject delCustomRequest(JSONObject requestJson) {
		JSONObject respJson = new JSONObject();
		
		String customID = (String)requestJson.get("customid".toString());

		LinkItem item = delCustomInLinksTab(customID);
		
		respJson.put("responseid",DELCUSTOM_REQUEST);
		respJson.put("customid", customID);
		
		if(item != null){ //成功

			respJson.put("code","0");
			JSONObject msgJson = new JSONObject();
			msgJson.put("port", Integer.toString(item.getPort()));
			msgJson.put("key", item.getPassword());//暂时未加密，升级处理
			msgJson.put("enabledate", item.getEnableDateStr());
			msgJson.put("enablembytes", Integer.toString(item.getEnableMBytes()));
			msgJson.put("feetype", Integer.toString(item.getFeeType()));
			msgJson.put("usedupmbytes", Integer.toString(item.getUsedUpMBytes()));
			msgJson.put("useddownmbytes", Integer.toString(item.getUsedDownMBytes()));
			msgJson.put("stopdate", item.getStopDateStr());
			respJson.put("msg", msgJson);
		
		} 
		else {
			
			respJson.put("code","1");
			respJson.put("msg","not find this customid.");
 			
		}
		
		return respJson;
		
	}

	/*
	 * 增流延时:成功返回当前流量信息
	 * 场景说明:充值,增加 剩余流量 或 上网天数, 以后根据流量套餐实际情况调整代码
	 * 请求报文: { "requestid":"102",
	 *          "orderid":"23080112343456", //防止重复充值, 增加14位“订单编号”,编号相同认为是同一请求的重复发送
	 *          "customID":"2023080223440",
	 *          "mbytes":"2000", //增加的流量,单位 M
	 *          "days":"20",  //增加的日期,单位天
	 *          "feeType":"1" //计费方式,
	 *         }
	 *         
	 * 应答报文: {"responseid":"102",
	 *         "orderid":"230801123434566",
	 *         "code":"0",//返回码:0成功 1用户不存在 2其他
	 *          msg:{ "customid":"2023080223440",
	 *                "enabledate":"230901",
	 *                "enablembytes":"12000",
	 *                "feetype":"1"
	 *                "usedupmbytes":"200",
	 *                "useddownmbytes":"1000"
	 *              }
	 *         } 
	 */ 

	private  JSONObject addFeeRequest(JSONObject requestJson) {
		JSONObject respJson = new JSONObject();
		
		String orderID = (String)requestJson.get("orderid".toString());
		String customID = (String)requestJson.get("customid".toString());
		int  mBytes = Integer.parseInt((String)requestJson.get("mbytes".toString()));
		int  days = Integer.parseInt((String)requestJson.get("days".toString()));
		int  feeType = Integer.parseInt((String)requestJson.get("feetype".toString()));

		int r = addEnableMbytesOrDateInLinksTab(orderID,customID,mBytes,days,feeType);
		
		respJson.put("responseid",ADDFEE_REQUEST);
		respJson.put("orderid", customID);
		
		if(r==0 || r==1){ //成功或重复充值，返回客户链接的基本信息

			respJson.put("code",r);
			
			LinkItem item=LinksTab.getLink(customID);
			JSONObject msgJson = new JSONObject();
			msgJson.put("customid", item.getCustomID());
			msgJson.put("enabledate", item.getEnableDateStr());
			msgJson.put("enablembytes", Integer.toString(item.getEnableMBytes()));
			msgJson.put("feetype", Integer.toString(item.getFeeType()));
			msgJson.put("usedupmbytes",Integer.toString(item.getUsedUpMBytes()));
			msgJson.put("useddownmbytes", Integer.toString(item.getUsedDownMBytes()));
			
			respJson.put("msg", msgJson);
		
		} 
		else {    //其他错误，返回2
			
			respJson.put("code","2");
			respJson.put("msg","some wrong.");
 			
		}		
		return respJson;		 
	}
	
	/*
	 * 查询链路:获取用户信息, 查询Linkstab获取
	 * 场景说明:定期获取所有custom的流量信息,
	 * 请求报文:{"requestid":"103",
	 *        "customids":["2023080223440","23023323232"]
	 *        }
	 * 应答报文:{"responseid":"103",
	 *         "code":"0",//0成功 1失败
	 *         "date":"20230802123000",//返回查询时间
	 *         msg:[{"customID":"2023080223440",
	 *               "enableMbytes":"20000",
	 *               "enableDate":"231009",
	 *               "linkType":"1",
	 *                "used"customID":"2023080223440",UpMbytes":"100",
	 *                "usedDownMbytes":"2000",
	 *                "stopDate":"10"
	 *               },
	 *               {"customID":"2023080223441",...},
	 *               {"customID":"2023080223442",...}
	 *               。。。  
	 *             ] 
	 *             
	 *        }
	 * 
	 */
  
	private JSONObject queryCustomRequest(JSONObject requestJson) {
		JSONObject respJson = new JSONObject();
		
		String customID = (String)requestJson.get("customid".toString());

		LinkItem item = delCustomInLinksTab(customID);
		
		respJson.put("responseid",DELCUSTOM_REQUEST);
		respJson.put("customid", customID);
		
		if(item != null){ //成功

			respJson.put("code","0");
			JSONObject msgJson = new JSONObject();
			msgJson.put("port", item.getPort());
			msgJson.put("key", item.getPassword());//暂时未加密，升级处理
			msgJson.put("enabledate", item.getEnableDateStr());
			msgJson.put("enablembytes", item.getEnableMBytes());
			msgJson.put("feetype", item.getFeeType());
			msgJson.put("usedupmbytes", item.getUsedUpMBytes());
			msgJson.put("useddownmbytes", item.getUsedDownMBytes());
			msgJson.put("stopdate", item.stopDate);
			respJson.put("msg", msgJson);
		
		} 
		else {
			
			respJson.put("code","1");
			respJson.put("msg","not find this customid.");
 			
		}
		
		return respJson;
			      	
	}
	
	//outputStream争用, 加锁
	private  synchronized void responseCharge(JSONObject respJson,OutputStream outputStream) {
		
		try {
		
			outputStream.write(respJson.toJSONString().getBytes(Constant.CHARSET));
		    outputStream.flush();
		 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

}