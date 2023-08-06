package press.gfw;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public  class LinkItem{
	public String customID;
	public int port=-1;
	public String password;
    public Date enableDate; 
	public int enableMBytes = 0;
	public int feeType=-1;
	public int usedUpMBytes = 0;
	public int usedDownMBytes = 0;
	public String lastAddFeeID = null;
	public Date stopDate=null;	
	
	public LinkItem() {

	}
	
	public LinkItem(String customID,int port,String password,Date enableDate,int enableMBytes,int feeType,int usedUpMBytes,int usedDownBytes,String lastAddFeeID, Date stopDate) {
         this.port = port; 
         this.customID = customID;
         this.enableDate = enableDate;
         this.stopDate = stopDate;
         this.enableMBytes = enableMBytes;
         this.usedUpMBytes = usedUpMBytes;
         this.usedDownMBytes =usedDownBytes;
		 this.password = password;
		 this.feeType = feeType;
		 this.lastAddFeeID = lastAddFeeID;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getCustomID() {
		return customID;
	}

	public void setCustomID(String customID) {
		this.customID = customID;
	}

	public Date getEnableDate() {
		return enableDate;
	}

	public String getEnableDateStr() {

		return Util.date2String(enableDate,"yyMMdd");
		
	}

	public void setEnableDate(Date enableDate) {
		this.enableDate = enableDate;
	}


	public String getStopDateStr() {
		return Util.date2String(stopDate,"yyMMddHHmm");
	}

	public Date getStopDate() {
		return stopDate;
	}

	public void setStopDate(Date stopDate) {
		this.stopDate = stopDate;
	}

	public int getEnableMBytes() {
		return enableMBytes;
	}

	public void setEnableMBytes(int enableMBytes) {
		this.enableMBytes = enableMBytes;
	}

	public int getUsedUpMBytes() {
		return usedUpMBytes;
	}

	public void setUsedUpMBytes(int usedUpMBytes) {
		this.usedUpMBytes = usedUpMBytes;
	}

	public int getUsedDownMBytes() {
		return usedDownMBytes;
	}

	public void setUsedDownMBytes(int usedDownMBytes) {
		this.usedDownMBytes = usedDownMBytes;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setFeeType(int feeType) {
		this.feeType = feeType;
	}

	public int getFeeType() {
		return feeType;
	}

	public String getLastAddFeeID() {
		return lastAddFeeID;
	}

	public void setLastAddFeeID(String lastAddFeeID) {
		this.lastAddFeeID = lastAddFeeID;
	}
	
}