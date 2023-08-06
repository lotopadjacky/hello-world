package press.gfw;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
public class LinksTab {
	
	//locks用的少，自动线程安全;globalLinks用的多，为效率手动控制
	private static Hashtable<String, String> linkLocks = new Hashtable<String, String>();//链路锁,锁用String类型
	public  static HashMap<String,LinkItem> globalLinks= null;//链路表
	private static Set<Integer> usedPorts = new HashSet<>(); 
	
	
    /*
     * 初始化链路表:服务启动时
     */
	public static void setLinksTab(HashMap<String,LinkItem> initLinks){

		globalLinks = initLinks;
		
		for(String customID : globalLinks.keySet()){
			linkLocks.put(customID, customID);//锁
			usedPorts.add(globalLinks.get(customID).getPort());//已用端口
		}
	}
	
	/*
	 * 增加链路:新用户第一次充值 或 用户转移线路
	 */
	public static synchronized int addLink(LinkItem item) {
		  
		 if(item == null ) {
			  return 1; 
		  }
	     String cid = item.customID;
	     int port = item.port;
	     if(globalLinks.get(cid)!=null) {
	    	  return 2; //用户已存在
	     }
	     if(port == 0) {
	  		   port = Util.getIdlePort(); 
				if (port < 0) {
					return 3; // 无端口
				}
		 }

	     globalLinks.put(cid, item);
		 linkLocks.put(cid,cid);
		 usedPorts.add(port);
		 
		 
		 return 0; 
	}
	
	/*
	 * 删除链路: 用户更换链路或清理长期不充值链路, 同时删除链路锁
	 */
	public static synchronized  LinkItem delLink(String customID) {
		   
		  LinkItem item = globalLinks.remove(customID);
		  
		  linkLocks.remove(customID);
		  usedPorts.remove(item.port);
		  
		  return item;
		
	}
	
	/*
	 * 增加可用流量: 用户充值. 对于指定线路线程安全
	 * dateType:增加时间的类型  1.按天   2.按月  
	 * numDayorMonth:数量
	 */
	public static int addEnableMbytesOrDate(String orderID,String customID ,int mbytes,int days,int feeType) {
		
		if(LinksTab.getLink(customID).getLastAddFeeID().equals(orderID)) {
			
			return 1; //重复充值,返回
			
		}
		
		synchronized (linkLocks.get(customID)) {
			
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(globalLinks.get(customID).getEnableDate());
			calendar.add(Calendar.DAY_OF_MONTH, days); //按天增加上网有效期,feeType暂时没用
			//if(feeType==1) calendar.add(Calendar.DAY_OF_MONTH, numDayorMonth); //增加月
			//if(feeType==2) calendar.add(Calendar.MONTH, numDayorMonth);//增加天
			globalLinks.get(customID).setEnableDate(calendar.getTime());
			
			globalLinks.get(customID).setEnableMBytes(globalLinks.get(customID).getEnableMBytes() + mbytes);
			
		}
		
		return 0;
	}
	
	/*
	 * 减少可用流量: 用户上网,对于指定线路要线程安全
	 */
	public static void minusEnableBytes(String customID,int mbytes,int type) {
		
		synchronized (linkLocks.get(customID)){	
	    
			globalLinks.get(customID).setEnableMBytes(globalLinks.get(customID).getEnableMBytes() - mbytes);
			
			if(type == 0) { 

				int up = globalLinks.get(customID).getUsedUpMBytes()+mbytes;
				globalLinks.get(customID).setUsedUpMBytes(up);
				
			} else {
				
				int down = globalLinks.get(customID).getUsedDownMBytes()+mbytes;
				globalLinks.get(customID).setUsedDownMBytes(down);
				
			}
		
		}
		
	}
	
	/*
	 * 判断链路是否可用:有流量且未到期
	 */
	public static boolean isEnableLink(String customID) {
		
		LinkItem linkItem=globalLinks.get(customID);
		
		Date today = new Date(); //频繁取时间, 要优化?
		Date endday = linkItem.getEnableDate();
				
		if((linkItem.getEnableMBytes() > 0) && (!today.after(endday))) {
		   
			return true;
	
		}
		
		return false;
	}
	
	/*
	 * 获取单条链路信息,获取剩余流量、已用流量 及 KEY信息等
	 */
	public static LinkItem getLink(String customID) {
		return globalLinks.get(customID);
	}
	
	/*
	 * 获取全部链路信息, 用于定期备份链路信息到本地文件或回传chargeserver。回传server只传：“剩余”、“已用”流量
	 */
	public static HashMap<String,LinkItem> getAllLinks() {
		return globalLinks;
	}
	
	public static Set<Integer> getUsedPorts(){
		
		 return usedPorts;
	}
	
}
    
    
