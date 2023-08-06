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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;

import java.util.Enumeration;

import java.util.Hashtable;


import javax.crypto.SecretKey;
import javax.net.ServerSocketFactory;

import org.json.simple.JSONObject;

/**
 *
 * GFW.Press服务器
 *
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 *
 */
public class Server extends Thread {

	public static void main(String[] args) throws IOException {

		Server server = new Server();

		server.service();

	}

	private File lockFile = null;

	private String proxyHost = "127.0.0.1"; // 默认为本机地址

	private int proxyPort = 3128; // 默认为HTTP代理标准端口

	private int listenPort = 0;
	
	private String customID = null;///

	private String password = null;///用户密码

	private SecretKey key = null; ///由password生成的通讯秘钥

	private Encrypt encrypt = null;

	private boolean kill = false;

	private Config config = null;

	private ServerSocket serverSocket = null;

    private int listenChargePort = 3028;// 监听计费服务器的端口缺省为3028
    
    private ServerSocket chargeServerSocket = null;
    
    
	/**
	 * 构造方法，主线程
	 */
	public Server() {

		lockFile = new File("server.lock");

		config = new Config();

		loadConfig(); // 获取配置参数

	}

	/**
	 * 构造方法，用户线程
	 *
	 * @param proxyHost
	 * @param proxyPort
	 * @param listenPort
	 * @param password
	 */
	public Server(String proxyHost, int proxyPort, int listenPort, String customID) {

		super();

		this.proxyHost = proxyHost;

		this.proxyPort = proxyPort;

		this.listenPort = listenPort;
		
		this.customID = customID;

		this.password = LinksTab.getLink(customID).getPassword();///

		encrypt = new Encrypt();

		if (encrypt.isPassword(this.password)) {

			key = encrypt.getPasswordKey(this.password);

		}

	}

	/**
	 * 构造方法，用户线程
	 *
	 * @param proxyHost
	 * @param proxyPort
	 * @param listenPort
	 * @param password
	 */
	public Server(String proxyHost, int proxyPort, String listenPort, String customID) {

		this(proxyHost, proxyPort, (listenPort != null && (listenPort = listenPort.trim()).matches("\\d+")) ? Integer.valueOf(listenPort) : 0, customID);

	}

	/**
	 * 暂停
	 *
	 * @param m
	 */
	private void _sleep(long m) {

		try {

			sleep(m);

		} catch (InterruptedException ie) {

		}

	}

	/**
	 * 获取密码
	 *
	 * @return
	 */
	public synchronized String getPassword() {

		return password;
	}

	/**
	 * @return the kill
	 */
	public synchronized boolean isKill() {

		return kill;

	}

	public synchronized void kill() {

		kill = true;

		if (serverSocket != null && !serverSocket.isClosed()) {

			try {

				serverSocket.close();

			} catch (IOException ex) {

			}

			serverSocket = null;

		}

	}

	/**
	 * 获取配置参数
	 */
	private void loadConfig() {

		JSONObject json = config.getServerConfig();

		if (json != null) {

			String _proxyHost = (String) json.get("ProxyHost");

			proxyHost = (_proxyHost == null || (_proxyHost = _proxyHost.trim()).length() == 0) ? proxyHost : _proxyHost;

			String _proxyPort = (String) json.get("ProxyPort");

			proxyPort = (_proxyPort == null || !(_proxyPort = _proxyPort.trim()).matches("\\d+")) ? proxyPort : Integer.valueOf(_proxyPort);

			String _listenChargePort =  (String) json.get("ListenChargePort");
			
			listenChargePort = (_listenChargePort== null || !(_listenChargePort = _listenChargePort.trim()).matches("\\d+")) ? listenChargePort : Integer.valueOf(_listenChargePort);

		}

	}
	/**
	 * 启动chargeServerLink
	 * 1) 启动时从chargeServer获取本服务器的 globalLinks;
	 * 2) chargeServer要求为用户增加流量
	 * 3) chargeServer要求删除用户
	 * 4) chargeServer要求增加用户
	 * 5) 定期为chargeServer 提供用户剩余流量数据
	 * 
	 *////
	private void comWithChargeServer(){

		try {
            //创建监听chargeServer的socket
			chargeServerSocket = ServerSocketFactory.getDefault().createServerSocket(listenChargePort);

		} catch (IOException ex) {

			log("监听计费服务器端口：" + listenChargePort + " 线程启动时出错，线程结束。");

			return;

		}
		
		ChargeThread chargeThread = new ChargeThread(chargeServerSocket);
		
		// startVirtualThread(serverThread);
		
		chargeThread.start();
		
	}
	
	/*
	 *定期存储链路表 
	 */
	private void startSaveLinksTab(){
		SaveLinksTabThread saveThread= new SaveLinksTabThread();
		saveThread.start();
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
	 * 用户线程
	 */
	@Override
	@SuppressWarnings("preview")
	public void run() {

		// log("监听端口：" + listenPort);

		if (encrypt == null || listenPort < 1024 || listenPort > 65536) {

			kill = true;

			log("监听端口：" + listenPort + " 线程参数不符合条件，线程结束。");

			return;

		}

		try {
             ///创建link监听服务进程，只要不删除用户就一直存在，流量=0时也存在
			serverSocket = ServerSocketFactory.getDefault().createServerSocket(listenPort);

		} catch (IOException ex) {

			kill = true;

			log("监听端口：" + listenPort + " 线程启动时出错，线程结束。");

			return;

		}

		while (!kill) {

            
            if(!LinksTab.isEnableLink(this.customID)) {
            	
            	_sleep(3000L);/// listenPort对应线路没有流量 或 超过日期, 退出
            	
            	break; 
            	
            }
			
			Socket clientSocket = null;

			try {

				clientSocket = serverSocket.accept();

			} catch (IOException ex) {

				if (kill) {

					break;

				}

				if (serverSocket != null && !serverSocket.isClosed()) {

					log("监听端口：" + listenPort + " 线程运行时出错，暂停3秒钟后重试。");

					_sleep(3000L);

					continue;

				} else {

					log("监听端口：" + listenPort + " 线程运行时出错，线程结束。");

					break;

				}

			}
			
			ServerThread serverThread = new ServerThread(clientSocket, proxyHost, proxyPort, key,customID);

			// startVirtualThread(serverThread);
			serverThread.start();

		}

		kill = true;

		if (serverSocket != null && !serverSocket.isClosed()) {

			try {

				serverSocket.close();

			} catch (IOException ex) {

			}

			serverSocket = null;

		}

	}

	/**
	 * 主线程
	 * 0)与chargeserver通讯，获取最新链路信息，初始化全局变量linkTab.  --zzq
	 * linkTab的其他使用点
	 * 1)增加流量或修改截期--chargeserver充值通讯
	 * 2)减少流量--每个下行加密 或 上行解密 线程跟踪修改剩余流量
	 * 3)读取--port建立通路、与squid建立通路、上/下行加解密线程 读取此值判断是否超额或超期
	 *///
	@SuppressWarnings("preview")
	public void service() {

		if (System.currentTimeMillis() - lockFile.lastModified() < 30 * 1000L) {

			log("服务器已经在运行中");

			log("如果确定没有运行，请删除 " + lockFile.getAbsolutePath() + "文件，重新启动");

			return;

		}

		try {

			lockFile.createNewFile();

		} catch (IOException ioe) {

		}

		lockFile.deleteOnExit();

		log("GFW.Press服务器开始运行......");

		log("代理主机：" + proxyHost);

		log("代理端口：" + proxyPort);

		Hashtable<String, String> users = null; /// 用户

		Hashtable<String, Server> threads = new Hashtable<>(); // 用户线程

		config.initLinksTab(); //读文件构造全局链路表,以后通过与chargeserver通讯完成
        
        startSaveLinksTab(); 
        
        comWithChargeServer();
					
		/*
		 * 循环读取内存链路表ZZQ
		 * 1)发现新增PORT链路，增加服务进程;
		 * 2)发现port属性变化，删除并新建服务进程;暂时只有秘钥变化
		 * 3)发现port删除，删除对应进程
		 */
		while (true) {

			lockFile.setLastModified(System.currentTimeMillis());

			users = config.getUser(); 
					
			//如果链路表为空，重新循环
			if (users == null) {

				_sleep(10 * 1000L); // 暂停10秒 ///没获取到users继续获取

				continue;

			}

			Enumeration<String> threadCustoms = threads.keys(); // 用户线程的所有端口,改port索引为customid索引

			while (threadCustoms.hasMoreElements()) { // 删除用户及修改密码处理 ///用户表更改:删除用户 或 修改密码

				String threadCustom = threadCustoms.nextElement();

				String userPassword = users.remove(threadCustom);
				//String userPassword = users.get(threadPort).getKey();

				if (userPassword == null) { // 用户已删除，清理该端口对应的服务器进程

					threads.remove(threadCustom).kill(); 

					log("删除用户：" + threadCustom);

				} else {

					Server thread = threads.get(threadCustom);

					if (!userPassword.equals(thread.getPassword())) { // 用户改密码:删除对应的线程，并建立新线程

						log("修改密码，用户：" + threadCustom);

						threads.remove(threadCustom);

						thread.kill();
                        
						thread = new Server(proxyHost, proxyPort, LinksTab.getLink(threadCustom).getPort(),threadCustom);

						threads.put(threadCustom, thread);

						// startVirtualThread(thread);
						thread.start();

					}

				}

			}

			Enumeration<String> customs = users.keys();

			while (customs.hasMoreElements()) { // 新用户:建立端口对应的新线程

				String customID = customs.nextElement();

				Server thread = new Server(proxyHost, proxyPort, LinksTab.getLink(customID).getPort(), customID);

				threads.put(customID, thread);

				// startVirtualThread(thread);
				thread.start();

			}

			users.clear();

			_sleep(20 * 1000L); // 暂停20秒

		}

	}

}
