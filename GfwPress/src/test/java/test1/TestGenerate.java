package test1;

import java.sql.Date;
import java.util.Calendar;
import java.util.Random;

import org.junit.jupiter.api.Test;

import press.gfw.Util;

public class TestGenerate {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
        String  s = Util.generatePassword();
		//System.out.print(s);
	
		Calendar calendar = Calendar.getInstance();
		System.out.println("Current Date = " + calendar.getTime());
		
	
	    calendar.add(Calendar.DAY_OF_MONTH, 1);
	    System.out.println("1天后 Date = " + calendar.getTime());
	    
	    calendar.add(Calendar.MONTH, 3);
	    System.out.println("再3月后 Date = " + calendar.getTime());	
	
	
	}
	

}
