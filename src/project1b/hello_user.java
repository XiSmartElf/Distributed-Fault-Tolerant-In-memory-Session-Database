package project1b;

import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.tagplugins.jstl.core.Set;

import project1b.View.ServerStatus;

@WebServlet("/EnterServlet")
public class hello_user extends HttpServlet {
	
	
	private static final long serialVersionUID = 1L;
	public static final int SESSION_TIMEOUT_SECS = 60;
	public static final int DELTA_DIFF_TIMEOUT_SECS = 3;

	private static final String cookieName = "CS5300PROJ1SESSION";
    private  Hashtable<String, SessionProperty> sessionStateTable;
    private RPC_Server rs = null;
    private RPC_Client rc = null;
    private View view = null;
    public static String serverID = null; //serverLocal
    private int sessionNum = 0;
    private Lock criticalSec = new ReentrantLock();
    
    //1: cookie MaxAge(-1) or 60secs
    //2: mutiple click too fast--> version is different too quick means fire two request at the same time then the version might be wrong. version+1 then next is different
    //3. db setup
    //4. randomly select any or UP ones
    
    /**
     *  Constructor to initialize the servlet
     * @throws InterruptedException 
     * @throws IOException 
     */  
    public hello_user() throws IOException, InterruptedException
    {
    	serverID = AddressResolver.getIpAddress();
    	sessionStateTable = new Hashtable<String, SessionProperty>(); 
    	GarbageCollection idCleaner = new GarbageCollection(sessionStateTable);
    	idCleaner.start();
    	rc = new RPC_Client();
    	View view = new View(serverID,rc);
    	view.start();
    	rs = new RPC_Server(sessionStateTable);
    	rs.start();
    }
    
    
    
    
    
    /**
     * doGet will call doPost
     */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		doPost(request,response);
	}

	
	
	
	
	
	/**
	 * doPost for processing the coming post back request
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/html");
		Cookie ck = retriveCookie(request);
		//if application cookie is not found due to either new session or user deletes cookie, return new page with new cookie
		if(ck==null){
			generateNewSite(request, response);
			response.getWriter().println("<p style=\"color:red\">no previous cookie found ( cookie out of date, or you terminated the session, or first time visit). New session is now created</p>");
			System.out.println("<MAIN> no cookie return to server so new session is generated");
			return;
		}
		
		//get session id, version number, and location meta data from cookie.
		String sessionId = ck.getValue().split(":::")[0];
		int version = Integer.parseInt(ck.getValue().split(":::")[1]);
		String metaData = ck.getValue().split(":::")[2];
		String srvPrimary = metaData.split(",")[0];
		String srvBackup = metaData.split(",")[1];
		System.out.println("<MAIN> This two server IPs are: "+srvPrimary+","+srvBackup);
		
		String whoFoundSession = null;
		String typeWhoFoundSess = null;
		//if this server doesn't have the requested session data, else the session is stored in this server and would continue execute
		if(!srvPrimary.equals(serverID) && !srvBackup.equals(serverID))
		{
			System.out.println("<MAIN> I(this server) am NOT one of the primary or backup server");
			Content rcvSessionData = null;
			rcvSessionData = rc.SessionReadClient(sessionId, srvPrimary).content;
			if(rcvSessionData ==  null){       	
				View.updateSrvView(srvPrimary, View.ServerStatus.DOWN, View.myView);
				if(!srvBackup.equals("SvrIDNULL"))
				{
					rcvSessionData = rc.SessionReadClient(sessionId, srvBackup).content;
					if(rcvSessionData==null)
						View.updateSrvView(srvBackup, View.ServerStatus.DOWN, View.myView);	
					else{
						View.updateSrvView(srvBackup, View.ServerStatus.UP, View.myView);	
						whoFoundSession = srvBackup;
						typeWhoFoundSess = "Backup";
					}
				}
			}
			else{
				View.updateSrvView(srvPrimary, View.ServerStatus.UP, View.myView);
				whoFoundSession = srvPrimary;
				typeWhoFoundSess = "Primary";
			}
			
			if(rcvSessionData==null){
				//bc time out UDP --> server fail or procedureCall failed, then there is no way to retrieve the requested session data, so we need to generate a error page
				response.getWriter().println("<p style=\"color:red\">"+serverID+" is serving your request. Tried retriving primary: "+srvPrimary+" and backup at "+srvBackup+"</p>");
				response.getWriter().println("<p style=\"color:red\">Backend server error! Maybe cause by server failure / internal failure. Sorry! Retry for a new session</p>");
				Cookie deleteCk=new Cookie("CS5300PROJ1SESSION","NULL");
				deleteCk.setMaxAge(0);
				response.addCookie(deleteCk);
				return;
			}	
			else
			{
				if(rcvSessionData.sessionData == null) //data is received but the session obtained is timed out already
				{
					generateNewSite(request, response);
					response.getWriter().println("<p style=\"color:red\">Your last session expired (deleted by the cleaner thread). New session is created</p>");
					return;
				}
				else //session data is successfully received and it's not expired
				{
					sessionStateTable.put(rcvSessionData.sessionData.sessionId, rcvSessionData.sessionData);
					srvPrimary = serverID;
					srvBackup = whoFoundSession;
				}	
			}	
		}
		else //if server is one of the metaData holder: meaning it has the corresponding session data
		{
			System.out.println("<MAIN> I(this server) am one of the primary or backup server");
			if(!srvPrimary.equals(serverID)) 
			{	
				whoFoundSession = srvBackup;
				typeWhoFoundSess = "Backup";
				srvBackup = srvPrimary;
				srvPrimary = serverID;
			}
			else
			{
				whoFoundSession = srvPrimary;
				typeWhoFoundSess = "Primary";
			}
			//if session expires and cookie has been deleted by the cleaner thread
			if(!sessionStateTable.containsKey(sessionId))
			{
				System.out.println("<MAIN> session has expired. generate new session (cleaner or teminated)");
				generateNewSite(request, response);
				response.getWriter().println("<p style=\"color:red\">Your last session expired (deleted by the cleaner thread). New session is created(i'm metadata)</p>");
				return;
			}
			//if session expires and still cookie exists and but not been deleted by the thread 
			if(sessionStateTable.get(sessionId).ex_timeStamp.before(GarbageCollection.getCurrentTime())){
				System.out.println("<MAIN> session has expired. generate new session (exist but expired)");
				sessionStateTable.remove(sessionId);
				generateNewSite(request, response);
				response.getWriter().println("<p style=\"color:red\">Session expired. New session is then created</p>");
				return;
			}
		}

		
		//Compare version number to ensure no failure or malicious modification to cookie
		if(version > sessionStateTable.get(sessionId).version)
		{
			response.getWriter().println("<p style=\"color:red\">cookie session version doesn't match server session version! Consider malicious modification to cookies...</p>");
			Cookie deleteCk=new Cookie("CS5300PROJ1SESSION","NULL");
			deleteCk.setMaxAge(0);
			response.addCookie(deleteCk);
			return;
		}
		//*************************************** Response actions--> session not expired****************************
		String act = request.getParameter("act");	
		if (act == null) {
		    //no button has been selected this is a fresh GET request**same as refresh request
			sessionStateTable.put(sessionId,constructSessProp(sessionId,sessionStateTable.get(sessionId).message));
			srvBackup = updateToBackup(srvBackup, sessionStateTable.get(sessionId));
			sendSite(request, response, sessionId, srvPrimary,srvBackup,whoFoundSession,typeWhoFoundSess);
		} 
		else if (act.equals("refresh")) {
			//Redisplay the session message, with an updated session expiration time;
			sessionStateTable.put(sessionId,constructSessProp(sessionId,sessionStateTable.get(sessionId).message));
			srvBackup = updateToBackup(srvBackup, sessionStateTable.get(sessionId));
			sendSite(request, response, sessionId, srvPrimary,srvBackup,whoFoundSession,typeWhoFoundSess);
		} 
		else if (act.equals("replace")) {
			//Replace the message with a new one (that the user typed into an HTML form field), and display the (new) message and expiration time;
			sessionStateTable.put(sessionId, constructSessProp(sessionId,request.getParameter("returnName")));
			srvBackup = updateToBackup(srvBackup, sessionStateTable.get(sessionId));	
			sendSite(request, response, sessionId, srvPrimary,srvBackup,whoFoundSession,typeWhoFoundSess);
		} 
		else if (act.equals("logout")) {
			//logs out session
			response.getWriter().println("Session logged out");
			sessionStateTable.remove(sessionId);
			Cookie deleteCk=new Cookie("CS5300PROJ1SESSION","NULL");
			deleteCk.setMaxAge(0);
			response.addCookie(deleteCk);
		} 
		else 
			response.getWriter().println("Invalid Operation!");	
	}

	
	
	
	
	
	
	
	/**
	 * @param request, response, sessionId
	 * @category for generating the site look and return to user
	 */
	public void sendSite(HttpServletRequest request, HttpServletResponse response, String sessionId, String srvPrimary, String srvBackup, String whoFound, String typeWhoFound) throws ServletException, IOException
	{
		String showMessage = sessionStateTable.get(sessionId).message+" .....Expires at: "+sessionStateTable.get(sessionId).client_timeStamp.toString()+ ". Session will be discarded at "+sessionStateTable.get(sessionId).ex_timeStamp;
		response.addCookie(sessionStateTable.get(sessionId).generateCookie(srvPrimary, srvBackup));
		request.setAttribute("message", showMessage);
		request.setAttribute("message2", serverID + " is serving your request");
		request.setAttribute("message3", "The session data is found at "+whoFound+" and it is the "+typeWhoFound);
		printView(request, response);
		request.setAttribute("cookieValue", sessionStateTable.get(sessionId).getCkValue(srvPrimary, srvBackup)); 
		request.getRequestDispatcher("/welcome.jsp").include(request, response);
	}
	
	
	
	
	
	
	
	/**
	 *  this is used to generate new session
	 * @param request, response
	 */
	public void generateNewSite(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		//build a new cookie property, put it in the table, generate a cookie and attach it to the response
		SessionProperty property = constructSessProp(null, null);
		String srvPrimary = serverID;
		String srvNewBack = getBackupServ(property);
		if(srvNewBack==null) srvNewBack="SvrIDNULL"; //this is really bad and shouldn't happen if GOSSIP works good and servers are most up.
		sessionStateTable.put(property.sessionId,property);
		response.addCookie(property.generateCookie(srvPrimary, srvNewBack));
		request.setAttribute("message", "Hello User!"+" .....Expires at: " + property.client_timeStamp.toString()+". Session will be discarded at "+property.ex_timeStamp);
		request.setAttribute("message2", serverID + " is serving your request");
		request.setAttribute("message3", "This is new session so session data is created at:"+serverID);
		printView(request, response);	
		request.setAttribute("cookieValue", property.getCkValue(srvPrimary, srvNewBack)); 
		request.getRequestDispatcher("/welcome.jsp").include(request, response);
	}
	
	
	
	
	
	
	
	/**
	 * @function: print view on the UI
	 */
	public void printView(HttpServletRequest request, HttpServletResponse response)
	{
		String message = "<p style=\"color:red\">Your View is the following:</p>";
		for(String server:View.myView.keySet())
			message+="<p style=\"color:blue\">"+server+"'s status is: "+View.myView.get(server).status+" ....at..."+ convertToTime(View.myView.get(server).timeStamp)+"</p>";
		request.setAttribute("message4", message);
		
		String message2 = "<p style=\"color:Red\">Your Session Table items now are the following:</p>";
		for(String sessionID:sessionStateTable.keySet())
			message2+="<p style=\"color:brown\">"+sessionID+"...version is: "+sessionStateTable.get(sessionID).version+"...expires: "+sessionStateTable.get(sessionID).client_timeStamp+"..message is: "+sessionStateTable.get(sessionID).message+"</p>";
		request.setAttribute("message5", message2);
		
	}	
	//** used for above function to display timestamp from milisec to time format
	private String convertToTime(long time)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");    
		Date resultdate = new Date(time);
		return sdf.format(resultdate).toString();
	}
	
	
	
	
	
	/**		
	 * 	function used to construct session state information/cookie property(class/object) 
	 *	either construct a new session property or from the post back cookie value
	 */
	public SessionProperty constructSessProp(String id, String mesag)
	{
		String sessionId=null;
		int version=0;
		String message=null;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND,SESSION_TIMEOUT_SECS); // add 60 seconds session expiration time
		Timestamp client_timeStamp = new Timestamp(calendar.getTime().getTime());	
		calendar.add(Calendar.SECOND,DELTA_DIFF_TIMEOUT_SECS); // add 3 seconds communication traveling tolerance
		Timestamp ex_timeStamp = new Timestamp(calendar.getTime().getTime());	
		
		if(id==null){ 
			//create new session state info/cookie 
			//UUID unique_id = UUID.randomUUID();
			criticalSec.lock();
			sessionId = serverID+"-"+Integer.toString(sessionNum);//unique_id.toString();
			sessionNum++;
			criticalSec.unlock();
			version = 0;
			message = "Hello User!";
		}
		else{ 
			//read cookie value from post back cookie value string
			sessionId = id;
			sessionStateTable.get(id).version++; //thread safe so no critical section problem for hashtable
			version = sessionStateTable.get(id).version;	
			message = mesag;
		}
		//build the session/cookie property
		SessionProperty property = new SessionProperty(sessionId, version, message, ex_timeStamp, client_timeStamp);
		return property;
	}	
	
	
	
	
	
	
	
	
	/**
	 * @category used to update the backup server to achieve 2 resilient 
	 * @param srvBackup, sessionId
	 */
	public String updateToBackup(String srvBackup, SessionProperty sp)
	{
		if(srvBackup.equals("SvrIDNULL"))
		{
			String srvNewBack = getBackupServ(sp);
			if(srvNewBack==null) srvNewBack="SvrIDNULL";
			return srvNewBack;
		}
		else
		{
			Content rvCont = rc.sessionWriteClient(srvBackup, sp).content;
			if(rvCont!=null && rvCont.returnCode == RPC_Code.returnWriteSuccess){
				View.updateSrvView(srvBackup, View.ServerStatus.UP, View.myView);
				return srvBackup;
			}
			else{
				View.updateSrvView(srvBackup, View.ServerStatus.DOWN, View.myView);
				return "SvrIDNULL";
			}
		}
	}	
	
	
	
	
	
	
	
	/**
	 * 		     find a backup server
	 * @param    session property to write 
	 * @return   new backup Server IP address
	 */
	public String getBackupServ(SessionProperty property)
	{
		//get new backup server address from the view (Amazon SimpleDB)
		//must be other than itself serverLOCAL, viewBackups can be a subSet of VIEW if there are too many nodes
		String srvNewBack = null;
		for(String backUp: View.myView.keySet())
		{
			if( backUp.equals(serverID))
				continue;
			Content rvCont = rc.sessionWriteClient(backUp, property).content;
			//if session write is successful we stop getting more response
			if(rvCont!=null && rvCont.returnCode == RPC_Code.returnWriteSuccess)
			{
				//successfully find a new backup for this new session
				srvNewBack = backUp;
				View.updateSrvView(backUp, View.ServerStatus.UP, View.myView);
				break;
			}
			else
			{
				View.updateSrvView(backUp, View.ServerStatus.DOWN, View.myView);
			}
		}
		return srvNewBack;
	}
	
	
	
	
	
	
	/**
	 * @param Cookies
	 * @return find the cookie that comes from this servlet based on cookie name
	 */
	public Cookie retriveCookie(HttpServletRequest request)
	{
		//check all cookies and find the application cookie
		Cookie[] cks = request.getCookies();
		Cookie ck = null;
		if(cks!=null){
			for(Cookie eachCk: cks){
				if(eachCk.getName().equals(cookieName)){
					ck=eachCk;
					break;
				}
			}
		}
		return ck;
	}
}
