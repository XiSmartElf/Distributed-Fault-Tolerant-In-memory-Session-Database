package project1b;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Hashtable;

import project1b.View.ServerStatus;


public class RPC_Server extends Thread {

	private static final int portProj1bRPC   =   5300;
	private Hashtable<String, SessionProperty> sessionStateTable = null;

	
	
	/**
	 * Constructor for initializing the session table
	 * @param table
	 */
	public RPC_Server(Hashtable<String, SessionProperty> table)
	{
		this.sessionStateTable=table;
	}
	
	
	
	

	@Override
	public void run()
	{
		DatagramSocket rpcSocket = null;
		try {
			rpcSocket = new DatagramSocket(portProj1bRPC);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		InetAddress returnAddr=null;
		while(true) {
			try
			{
				Content recvCont = null;
				byte[] inBuf = new byte[1000];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				rpcSocket.receive(recvPkt);
				ByteArrayInputStream input = new ByteArrayInputStream(inBuf);
				ObjectInputStream object = new ObjectInputStream(input);
				returnAddr = recvPkt.getAddress();
				System.out.println(returnAddr.toString().substring(1));
				recvCont = (Content)object.readObject();
				int returnPort = recvPkt.getPort();
				// here inBuf contains the callID and operationCode
				int operationCode = recvCont.operCode; // get requested operationCode
				System.out.println("<Server> Sever received packet the code is: "+operationCode+", the call ID is: "+recvCont.callId+" from ip: "+ returnAddr.toString().substring(1));
				View.updateSrvView(returnAddr.toString().substring(1), ServerStatus.UP, View.myView);
				View.updateSrvView(hello_user.serverID, ServerStatus.UP, View.myView);
				byte[] outBuf = null;
				switch(operationCode) {
					case RPC_Code.operationSESSIONREAD:
						// SessionRead accepts call args and returns call results 
						outBuf = SessionRead(recvCont);
						System.out.println("<Server> successfully read from table!");
						break;
					case RPC_Code.operationSESSIONWRITE:
						outBuf = sessionWrite(recvCont);
						System.out.println("<Server> successfully write to table!");
						break;
					case RPC_Code.operationExchangeView:
						outBuf = exchangeView(recvCont);
						System.out.println("<Server> successfully exchange table! Going to send it back to "+returnAddr.toString().substring(1));
						break;
				}
				// here outBuf should contain the callID and results of the call
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sendPkt);
			}
			catch(Exception ee)
			{
				ee.printStackTrace();
				System.out.println("<Server> receiving failure! from ip: "+returnAddr.toString().substring(1));
			}
		}
	}

	
	
	
	
	
	/**
	 * @category read session data from table
	 * @param content
	 */
	public byte[] SessionRead(Content content) 
	{
		//request sessionRead
		System.out.println("<Server> start reading from table now....for session id: "+ content.sessionId);
		Content data;
		//for(String session:sessionStateTable.keySet())
			//System.out.println(session);
		if(sessionStateTable.containsKey(content.sessionId)){
			System.out.println("<Server> My table contains this session");

			//build new content with the requested sessionData return back to client
			//maybe expired but hasn't been cleaned
			if(sessionStateTable.get(content.sessionId).ex_timeStamp.before(GarbageCollection.getCurrentTime()))
			{
				data = new Content(content.callId, null);
				System.out.println("<Server> but this requested session read data is timeout is NULL is sent");
			}
			else
			{
				data = new Content(content.callId, sessionStateTable.get(content.sessionId));	
				System.out.println("<Server> this requested session read data is SENT");

			}
		}
		else{
			//session timeout so doesn't exist in this server
			data = new Content(content.callId, null);
			System.out.println("<Server> this requested session read data is timeout is NULL is sent");
		}
					
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(buffer);
			out.writeObject(data);
			out.close();
			buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toByteArray();	
	}
	
	
	
	
	
	/**
	 * @category write session data to table
	 * @param content
	 */
	public byte[] sessionWrite(Content content)
	{
		SessionProperty sp = content.sessionData;
		sessionStateTable.put(sp.sessionId, sp);
		//if successfully rewrite to the table
		Content data = new Content(content.callId, RPC_Code.returnWriteSuccess);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(buffer);
			out.writeObject(data);
			out.close();
			buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toByteArray();
	}

	
	
	
	
	
	
	/**
	 * @category exchange VIEW data
	 * @param content
	 */
	public byte[] exchangeView(Content content)
	{
		//merge the received view and my current view
		View.mergeView(View.convertRecvView(content.myView),View.myView);
		
		//if successfully merge to the table then send the merged view back to the client
		Content data = new Content(content.callId, RPC_Code.operationExchangeView, View.myView);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(buffer);
			out.writeObject(data);
			out.close();
			buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toByteArray();
	}
	
	
}
