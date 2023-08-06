package press.gfw;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;


/*
 *  定期保存文件 
 */
public class SaveLinksTabThread extends Thread {
	
	private String filePath = Constant.FILE_LINKSTAB;
	
	public SaveLinksTabThread() {
		
	
	}

	private void _sleep(long ms) {
		try {
			
			sleep(ms);
		
		} catch (InterruptedException e) {

		}
	}
	

	/*
	 * 输出文本到文件:FileOutputStream方式
	 */
	private boolean writeGlobalLinks2File(String filePath) {
	  
	  File f=new File(filePath);
	  FileOutputStream fopStream=null;
	  OutputStreamWriter opsWriter=null;
	  StringBuilder sb=new StringBuilder();
	  	  
	  try {
		fopStream = new FileOutputStream(f);
	} catch (FileNotFoundException e) {
		e.printStackTrace();
		return false;
	}
	 
	try {
	
		opsWriter = new  OutputStreamWriter(fopStream,"UTF-8");
	
	} catch (UnsupportedEncodingException e) {
		
		e.printStackTrace();
		
		try {
			fopStream.close();
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		
		return false;
	}
	
	try {
	  for(LinkItem item:LinksTab.getAllLinks().values()) {
		    	String str =null;
				str  = item.getCustomID()+" ";
				str += item.getPort()+" ";
			    str += item.getPassword();
				str += item.getEnableDateStr()+" ";
			    str += item.getEnableMBytes()+" ";
			    str += item.getFeeType()+" ";
			    str += item.getUsedUpMBytes()+" ";
			    str += item.getUsedDownMBytes()+" ";
				str += item.getLastAddFeeID()+" ";
				str += item.getStopDateStr()+"\\r\\n";
				sb.append(str);
	      }	  
	  opsWriter.append(sb.toString());
	  opsWriter.flush();
	  
	   return true;

	} catch (IOException e) {
		e.printStackTrace();
	} finally {

		try {
    	
			if(opsWriter!=null) opsWriter.close();
  	        if(fopStream!=null) fopStream.close();
		 
		} catch (IOException e) {

			 e.printStackTrace();
		
		}

	  }
	
	  return false;
	}
	
	
	@Override
	@SuppressWarnings("preview")
	public void run() {
	   
		while(true) {
		     
			//存储到文件中
			if(!writeGlobalLinks2File(filePath)) {
				  break;
			}
			
			//等待30秒，之后再存储
			_sleep(1000*10);
			
			///退出循环的控制
			//if(tempExit()) break;
		}
		
	}
	
}