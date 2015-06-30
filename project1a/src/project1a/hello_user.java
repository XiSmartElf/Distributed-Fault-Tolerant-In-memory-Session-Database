package project1a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.tagplugins.jstl.core.Set;

@WebServlet("/EnterServlet")
public class hello_user extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static Hashtable<String, SessionProperty> sessionStateTable = new Hashtable<String, SessionProperty>();  
   
    public hello_user()
    {
    	//constructor creates a thread that deletes expired session from the hashTable every
    	//it checks every 10 secs
    	Thread idCleaner = new Thread(){
    		@Override
    		public void run()
    		{
    			while(true)
    			{
    				List<String>deleteSet = new LinkedList<String>();
	    			for(String id: sessionStateTable.keySet())
	    			{
	    				if(sessionStateTable.get(id).ex_timeStamp.before(getCurrentTime())==true){
	    					deleteSet.add(id);
	    				}
	    			}
	    			for(String id: deleteSet)
	    				sessionStateTable.remove(id);
	    			try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	};
    	idCleaner.start();
    	
    	
    }
    
	public static String getIpAddress() throws IOException, InterruptedException
	{
		Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec("/opt/aws/bin/ec2-metadata --public-ipv4");
        BufferedReader stdInput = new BufferedReader(new 
        InputStreamReader(proc.getInputStream()));

       	BufferedReader stdError = new BufferedReader(new 
        InputStreamReader(proc.getErrorStream()));
       	String s = null;
       	while ((s = stdInput.readLine()) != null) {
            return s.split(" ")[1];
       	}
       	return null;
	}
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request,response);
	}
	
	public void generateNewSite(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		//build a new cookie property, put it in the table, generate a cookie and attach it to the response
		SessionProperty property = constructCkProp(null,0, null);
		sessionStateTable.put(property.sessionId,property);
		response.addCookie(property.generateCookie());
		request.setAttribute("message", "Hello User!"+" .....Expires at: " + property.ex_timeStamp.toString());
		request.setAttribute("cookieValue", property.getCkValue()); 
		try {
			request.setAttribute("message",getIpAddress());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.getRequestDispatcher("/welcome.jsp").include(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		Cookie[] cks = request.getCookies();
		Cookie ck = null; 
		//check all cookies and find the application cookie
		if(cks!=null){
			for(Cookie eachCk: cks){
				if(eachCk.getName().equals("CS5300PROJ1SESSION")){
					ck=eachCk;
					break;
				}
			}
		}
		//if application cookie is not found due to either new session or user deletes cookie, return new page with new cookie
		if(ck==null){
			generateNewSite(request, response);
			response.getWriter().println("<p style=\"color:red\">no previous cookie found. New session is created</p>");
			return;
		}
		//get id and version number from cookie.
		String sessionId = ck.getValue().split(":::")[0];
		int version = Integer.parseInt(ck.getValue().split(":::")[1]);
		//String metaData = ck.getValue().split(":::")[2];
		
		//if session expires and cookie has been deleted by the cleaner thread
		if(!sessionStateTable.containsKey(sessionId))
		{
			generateNewSite(request, response);
			response.getWriter().println("<p style=\"color:red\">Your last session expired (deleted by the cleaner thread) or you terminated the session. New session is created</p>");
			return;
		}
		//if session expires and still cookie exists and but not been deleted by the thread 
		if(sessionStateTable.get(sessionId).ex_timeStamp.before(getCurrentTime())){
			sessionStateTable.remove(sessionId);
			generateNewSite(request, response);
			response.getWriter().println("<p style=\"color:red\">Session expired. New session is then created</p>");
			return;
		}
		//*************************************** Response actions--> session not expired****************************
		String act = request.getParameter("act");	
		if (act == null) {
		    //no button has been selected this is a fresh GET request**same as refresh request
			sessionStateTable.put(sessionId,constructCkProp(sessionId,sessionStateTable.get(sessionId).version,sessionStateTable.get(sessionId).message));
			String showMessage = sessionStateTable.get(sessionId).message+" expires at: "+sessionStateTable.get(sessionId).ex_timeStamp.toString();
			response.addCookie(sessionStateTable.get(sessionId).generateCookie());
			request.setAttribute("message", showMessage);
			request.setAttribute("cookieValue", sessionStateTable.get(sessionId).getCkValue()); //*****
			try {
				request.setAttribute("message",getIpAddress());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request.getRequestDispatcher("/welcome.jsp").include(request, response);
		} 
		else if (act.equals("refresh")) {
			//Redisplay the session message, with an updated session expiration time;
			sessionStateTable.put(sessionId,constructCkProp(sessionId,sessionStateTable.get(sessionId).version,sessionStateTable.get(sessionId).message));
			String showMessage = sessionStateTable.get(sessionId).message+" .....Expires at: "+sessionStateTable.get(sessionId).ex_timeStamp.toString();
			response.addCookie(sessionStateTable.get(sessionId).generateCookie());
			request.setAttribute("message", showMessage);
			request.setAttribute("cookieValue", sessionStateTable.get(sessionId).getCkValue()); //*****
			try {
				request.setAttribute("message",getIpAddress());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request.getRequestDispatcher("/welcome.jsp").include(request, response);
		} 
		else if (act.equals("replace")) {
			//Replace the message with a new one (that the user typed into an HTML form field), and display the (new) message and expiration time;
			sessionStateTable.put(sessionId, constructCkProp(sessionId,sessionStateTable.get(sessionId).version,request.getParameter("returnName")));
			String showMessage = sessionStateTable.get(sessionId).message+" .....Expires at: "+sessionStateTable.get(sessionId).ex_timeStamp.toString();
			response.addCookie(sessionStateTable.get(sessionId).generateCookie());
			request.setAttribute("message", showMessage);		
			request.setAttribute("cookieValue", sessionStateTable.get(sessionId).getCkValue()); //*****
			try {
				request.setAttribute("message",getIpAddress());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request.getRequestDispatcher("/welcome.jsp").include(request, response);	
		} 
		else if (act.equals("logout")) {
			//logs out session
			response.getWriter().println("Session logged out");
			sessionStateTable.remove(sessionId);
		} 
		else 
			response.getWriter().println("Invalid Operation!");	
	}
	
	public Timestamp getCurrentTime()
	{ 
		//function used to get current time
		Calendar calendar = Calendar.getInstance();
		return new Timestamp(calendar.getTime().getTime());	
	}
	
	public SessionProperty constructCkProp(String id, int ver, String mesag)
	{
		//function used to construct session state information/cookie property(class/object) 
		//either construct a new session property or from the postback cookie value
		String sessionId=null;
		int version=0;
		String message=null;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE,1); // add 3 mins session expiration time
		Timestamp ex_timeStamp = new Timestamp(calendar.getTime().getTime());	
		
		if(id==null){ 
			//create new session state info/cookie 
			UUID unique_id = UUID.randomUUID();
			sessionId = unique_id.toString();
			version = 0;
			message = "Hello User!";
		}
		else{ 
			//read cookie value from postback cookie value string
			sessionId = id;
			version = ver+1;	
			message = mesag;
		}
		//build the session/cookie propert
		SessionProperty property = new SessionProperty(sessionId, version, message, ex_timeStamp);
		return property;
	}
	
	
	//session state info/cookie property class used to store session state information and build cookie to send to user
	public class SessionProperty
	{
		String sessionId;
		int version;
		String message;
		Timestamp ex_timeStamp;
		//String metaData;
		
		//constructor to set session state info
		public SessionProperty(String id, int version, String mesg, Timestamp time)
		{
			this.sessionId = id;
			this.version = version;
			this.message = mesg;
			this.ex_timeStamp = time;
		}
		//build cookie value string from sessions state info
		public String getCkValue(){
			return sessionId+":::"+Integer.toString(version)+":::"+"metaData";
		}
		//generate a cookie file for sending to user
		public Cookie generateCookie()
		{
			Cookie ck=new Cookie("CS5300PROJ1SESSION","NULL");
			ck.setMaxAge(-1);
			ck.setValue(getCkValue());		
			return ck;
		}
	}
	

}
