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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Timestamp;

import javax.crypto.SecretKey;
import javax.net.SocketFactory;

/**
 *
 * GFW.Press服务器线程
 *
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 *
 */
public class ServerThread extends PointThread {

	private String proxyHost = null;

	private int proxyPort = 0;

	private Socket clientSocket = null;

	private Socket proxySocket = null;
	
	//private int linkID = 0; ///

	private SecretKey key = null;
	
	private String customID = null;

	private int overN = 0;

	private InputStream clientIn = null;

	private OutputStream clientOut = null;

	private InputStream proxyIn = null;

	private OutputStream proxyOut = null;

	public ServerThread(Socket clientSocket, String proxyHost, int proxyPort, SecretKey key,String customID) {

		this.clientSocket = clientSocket;
		
		//this.linkID = customID; ///

		this.proxyHost = proxyHost;

		this.proxyPort = proxyPort;

		this.key = key;

		this.customID =customID;

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

	private void close() {

		close(clientIn);

		close(proxyOut);

		close(proxyIn);

		close(clientOut);

		close(clientSocket);

		close(proxySocket);

	}

	private void close(Closeable o) {

		if (o == null) {

			return;

		}

		try {

			o.close();

		} catch (IOException e) {

		}

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

	@Override
	public synchronized void over() {

		overN++;

		if (overN < 2) {
            
			if(LinksTab.isEnableLink(customID)) { //超额即中断
			      return;
			}

		}

		_sleep(OVER_TIMEOUT);


		///线程日志
		String logstr = "ServerThread:"+Thread.currentThread().getName();
		logstr += ",cSocket:"+clientSocket.getPort()+" "+clientSocket.getLocalPort();
		logstr += ",pSockdt:"+proxySocket.getLocalPort()+" "+proxySocket.getPort();
		logstr += "->close!!";
		log(logstr);

		close();
	}

	/**
	 * 启动服务器与客户端之间的转发线程，并对数据进行加密及解密
	 * 链路线程带有链路参数,将继续传播到上行、下行线程中 --zzq
	 */
	@Override
	@SuppressWarnings("preview")
	public void run() {
       
		try {
			
			// 连接代理服务器
			proxySocket = SocketFactory.getDefault().createSocket();

			proxySocket.connect(new InetSocketAddress(proxyHost, proxyPort), CONN_TIMEOUT);

			proxySocket.setSoTimeout(SOCK_TIMEOUT);
			clientSocket.setSoTimeout(SOCK_TIMEOUT);

			proxySocket.setTcpNoDelay(true);
			clientSocket.setTcpNoDelay(true);

			// 打开 keep-alive
			proxySocket.setKeepAlive(true);
			clientSocket.setKeepAlive(true);

			// 获取输入输出流
			clientIn = clientSocket.getInputStream();
			clientOut = clientSocket.getOutputStream();

			proxyIn = proxySocket.getInputStream();
			proxyOut = proxySocket.getOutputStream();

		} catch (IOException ex) {

			log("连接代理服务器出错：" + proxyHost + ":" + proxyPort);

			close();

			return;

		}
		///线程日志
		String logstr = "ServerThread:"+Thread.currentThread().getName();
		logstr += ",cSocket:"+clientSocket.getPort()+" "+clientSocket.getLocalPort();
		logstr += ",pSockdt:"+proxySocket.getLocalPort()+" "+proxySocket.getPort();
		logstr += "->start...";
		log(logstr);

      
		
		//解密发送线程启动，传递链路参数，在解密线程中跟踪，控制流量  --zzq
		DecryptForwardThread forwardProxy = new DecryptForwardThread(this, clientIn, proxyOut, key, customID);
		// startVirtualThread(forwardProxy);
		forwardProxy.start();

		//加密接收线程启动，在加密线程中跟踪，控制流量，传递链路参数  --zzq
		EncryptForwardThread forwardClient = new EncryptForwardThread(this, proxyIn, clientOut, key, customID);
		// startVirtualThread(forwardClient);
		forwardClient.start();

	}

}
