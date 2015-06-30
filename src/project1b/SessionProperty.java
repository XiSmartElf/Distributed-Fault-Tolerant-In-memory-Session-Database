package project1b;
import java.io.Serializable;
import java.sql.Timestamp;

import javax.servlet.http.Cookie;


public class SessionProperty implements Serializable {
	/**
	 ** session state info/cookie property class used to store session state information
	 ** and build cookie to send to user
	 **/
	
	String sessionId;
	int version;
	String message;
	Timestamp ex_timeStamp;
	Timestamp client_timeStamp;
	//String metaData;
	
	//constructor to set session state info
	public SessionProperty(String id, int version, String mesg, Timestamp discardTime, Timestamp client_disCardtime)
	{
		this.sessionId = id;
		this.version = version;
		this.message = mesg;
		this.ex_timeStamp = discardTime;
		this.client_timeStamp = client_disCardtime;
	}
	//build cookie value string from sessions state info
	public String getCkValue(String srvPrimary, String srvNewBack){
		return sessionId+":::"+Integer.toString(version)+":::"+srvPrimary+","+srvNewBack;
	}
	//generate a cookie file for sending to user
	public Cookie generateCookie(String srvPrimary, String srvNewBack)
	{
		Cookie ck=new Cookie("CS5300PROJ1SESSION","NULL");
		ck.setMaxAge(hello_user.SESSION_TIMEOUT_SECS + hello_user.DELTA_DIFF_TIMEOUT_SECS);
		ck.setValue(getCkValue(srvPrimary, srvNewBack));		
		return ck;
	}
}
