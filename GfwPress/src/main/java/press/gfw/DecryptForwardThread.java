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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;

import javax.crypto.SecretKey;

/**
 * GFW.Press解密及转发线程
 *
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 *
 */
public class DecryptForwardThread extends Thread {

	private static final int BUFFER_SIZE_MAX = 1024 * 768; // 缓冲区可接受的最大值，768K
	
	private PointThread parent = null;

	private InputStream inputStream = null;

	private OutputStream outputStream = null;

	private Encrypt aes = null;

	private SecretKey key = null;
	
	//private int linkID = 0; ///
	private String customID = null;
	
	private int counterBytes = 0;///

	/**
	 * 构造方法
	 *
	 * @param parent       父线程
	 * @param inputStream  输入流
	 * @param outputStream 输出流
	 *
	 */
	public DecryptForwardThread(PointThread parent, InputStream inputStream, OutputStream outputStream, SecretKey key,String customID) {

		this.parent = parent;

		this.inputStream = inputStream;

		this.outputStream = outputStream;

		this.key = key;
		
		//this.linkID = linkID; ///
        this.customID = customID;
        
		aes = new Encrypt();

	}

	/**
	 * 打印信息
	 *
	 * @param o
	 */
	@SuppressWarnings("unused")
	private void log(Object o) {

		String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);

		System.out.println("[" + time + "] " + o.toString());

	}

	/**
	 * 解密转发
	 * 此处实现流量跟踪、断网判断
	 * 流量跟踪:累计上传流量，循环体内超过100M则回写到数组(目的是减少写的次数，代价是少控制100M流量)
	 * 断网判断:同时做流量判断，超时或超额则断网
	 */
	@Override
	public void run() {

		byte[] buffer = null;

		byte[] size_bytes = null;

		int[] sizes = null;

		byte[] decrypt_bytes = null;
		
		counterBytes = 0;

		while (true) {

			buffer = new byte[Encrypt.ENCRYPT_SIZE];

			int read_num = 0;

			try {

				read_num = inputStream.read(buffer);

			} catch (IOException ex) {

				break;

			}

			if (read_num != Encrypt.ENCRYPT_SIZE) {

				break;

			}

			size_bytes = aes.decrypt(key, buffer);

			if (size_bytes == null) {

				break; // 解密出错，退出

			}

			sizes = aes.getBlockSizes(size_bytes);

			if (sizes == null || sizes.length != 2 || sizes[0] > BUFFER_SIZE_MAX) {

				break;

			}

			int size_count = sizes[0] + sizes[1];

			buffer = new byte[size_count];

			int read_count = 0;

			while (read_count < size_count) {

				try {

					read_num = inputStream.read(buffer, read_count, size_count - read_count);

				} catch (IOException ex) {

					break;

				}

				if (read_num == -1) {

					break;

				}

				read_count += read_num;

			}

			if (read_count != size_count) {

				break;

			}

			if (sizes[1] > 0) { // 如果存在噪音数据

				byte[] _buffer = new byte[sizes[0]];

				System.arraycopy(buffer, 0, _buffer, 0, _buffer.length);

				decrypt_bytes = aes.decrypt(key, _buffer);

			} else {

				decrypt_bytes = aes.decrypt(key, buffer);

			}

			if (decrypt_bytes == null) {

				break;

			}

			try {

				outputStream.write(decrypt_bytes);

				outputStream.flush();

			} catch (IOException ex) {

				break;

			}
			
			int type = 0; //类型-上行流量: 网站<-用户

			//记录流量:decrypt_bytes里是最后明文上网数据,去除噪音并解密
			//超额则停止链路。为减少互斥写次数，达到COUNTER_MAX时才更新全局链路表
			counterBytes += decrypt_bytes.length;;
			
			if(counterBytes >= Constant.COUNTER_MAX ) {
			
				int minusNum  = counterBytes / Constant.MBYTES_NUM;
				
				counterBytes = counterBytes % Constant.MBYTES_NUM;
				
				LinksTab.minusEnableBytes(customID,minusNum,type); 
				
				//log
				log(Thread.currentThread().getName()+":用户="+ customID +",上行:("+minusNum+"M."+counterBytes+"B) 剩余:"+LinksTab.getLink(customID).getEnableMBytes()+"M");
			}
			
           ///判断流量，同port所有并发clinet-proxy进程,只要一个先用光流量，其他连接应立即终止 
			if(!LinksTab.isEnableLink(customID)) {
				break; //流量超额 或 超期 ，退出
			}
			

		}

		buffer = null;

		size_bytes = null;

		sizes = null;

		decrypt_bytes = null;

		parent.over();

	}

}
