package press.gfw;

/*
 * 常量类
 * 
 */

public class Constant {

	
	public static final int STR_BUFFER_MIN = 1024 * 16;//

	public static final int STR_BUFFER_MAX = 1024 * 32;//
	
	public static final int COUNTER_MAX =  1024 * 1024 * 2; /// 流量控制:累计到此值时链路表更新剩余流量

	public static final int MBYTES_NUM = 1024 * 1024; ///流量控制:链路表以M为单位记录剩余流量
	
	public static final int PORT_START = 10000; ///开始端口
	
	public static final int PORT_END = 20000; ///结束端口
	
	public static final String FILE_LINKSTAB = "userbytes.txt"; //linkstab文件
	
	public static final int CONN_TIMEOUT = 3000;

	public static final int SOCK_TIMEOUT = 3000;

	public final static int OVER_TIMEOUT = 300;
  
	public final static String CHARSET  = "UTF-8"; 
}
