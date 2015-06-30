package project1b;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * Content for each RPC packet
 */
public class Content implements Serializable
{
	private static final long serialVersionUID = 9001348407102545884L;
	int callId;
	int operCode;
	String sessionId;
	SessionProperty sessionData;
	int returnCode;
	Hashtable<String,StatusTuple>  myView;
	

	//for RPC Client to request session data (session read request)
	public Content(int callID, int operationCode, String sessionID)
	{
		this.callId = callID;
		this.operCode = operationCode;
		this.sessionId = sessionID;
	}
	
	//for returning the session data used by the RPC server for returning data for a session read request
	public Content(int callID , SessionProperty sessionData)
	{
		this.callId = callID;
		this.sessionData = sessionData;
	}
	
	//for RPC Client to request a session write action to RPC Server
	public Content(int callID , int operationCode, SessionProperty sessionData)
	{
		this.callId = callID;
		this.operCode = operationCode;
		this.sessionData = sessionData;
	}
	
	//for return successful code or error code
	public Content(int callID, int code)
	{
		this.callId = callID;
		this.returnCode = code;
	}
	
	
	//exchange view between server and client. They both use this to exchange data
	public Content(int callID, int operationCode, Hashtable<String,StatusTuple> view)
	{
		this.callId = callID;
		this.operCode = operationCode;
		this.myView = view;
	}
}
