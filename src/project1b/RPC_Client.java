package project1b;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class RPC_Client
{
	private static final int portProj1bRPC = 5300;
	private static final int TIME_OUT_EXCHANGE = 400;
	private static final int TIME_OUT_READ_WRITE = 300;

	private int callId =0; //the callID increments after every call for a server	
	private final Lock criticalSec = new ReentrantLock();
	
	
	
	/**
	 * Session read from client --> calls procedure call
	 * @param sessionID,  destAddr
	 * @return Content
	 */
	public Tuple SessionReadClient(String sessionID, String destAddr)
	{
		criticalSec.lock();
		callId++;
		int callID = this.callId;
		criticalSec.unlock();
		System.out.println("SessionReadClient is called between me("+hello_user.serverID+"), with "+destAddr);
		int operCode = RPC_Code.operationSESSIONREAD;
		// Construct the datagram packet			
		Content content = new Content(callID,operCode,sessionID);
		return procedureCall(content, destAddr, callID, TIME_OUT_READ_WRITE);
	}
	
	
	
	
	
	/**
	 * Session write from client --> calls procedure call
	 * @param sessionID,  session data
	 * @return Content
	 */
	public Tuple sessionWriteClient(String destAddr, SessionProperty sp)
	{
		criticalSec.lock();
		callId++;
		int callID = this.callId;
		criticalSec.unlock();
		System.out.println("sessionWriteClient is called between me("+hello_user.serverID+"), with "+destAddr);
		int operCode = RPC_Code.operationSESSIONWRITE;
		// Construct the datagram packet			
		Content content = new Content(callID,operCode, sp);
		return procedureCall(content, destAddr,callID, TIME_OUT_READ_WRITE);
	}
	
	
	
	
	
	
	/**
	 * Session write from client --> calls procedure call
	 * @param sessionID,  session data
	 * @return Content
	 */
	public Tuple ExchangeViews(String destAddr, Hashtable<String,StatusTuple> view)
	{
		criticalSec.lock();
		callId++;
		int callID = this.callId;
		criticalSec.unlock();
		System.out.println("exchangeView is called between me("+hello_user.serverID+"), with "+destAddr+" ..this call ID is "+callID);
		int operCode = RPC_Code.operationExchangeView;
		// Construct the datagram packet			
		Content content = new Content(callID, operCode, view);
		return procedureCall(content, destAddr,callID, TIME_OUT_EXCHANGE);
	}
	
	
	
	
	
	
	
	
	/**
	 * Procedure call function
	 * @param call ID,  destAddr, Content to deliver
	 * @return Content
	 */
	private Tuple procedureCall(Content content, String destAddr, int ID, int timeOutLength) 
	{
		Content recvCont = null;
		String returAddres = null;
		try
		{
	        // Construct the socket and write sending object
			DatagramSocket rpcSocket = new DatagramSocket();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(buffer);
			out.writeObject(content);
			out.close();
			buffer.close();
			//send data to destination
		    InetAddress host = InetAddress.getByName(destAddr);
			DatagramPacket sendPkt = new DatagramPacket(buffer.toByteArray(), buffer.size(), host, portProj1bRPC);
			rpcSocket.send(sendPkt);
			//try to receive response
			byte [] inBuf = new byte[1000];
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			rpcSocket.setSoTimeout(timeOutLength);
			//Get the first response from the servers
			while(true)
			{
				try {
					do {
						recvPkt.setLength(inBuf.length);
						rpcSocket.receive(recvPkt);
						returAddres = recvPkt.getAddress().toString();
					    ByteArrayInputStream input = new ByteArrayInputStream(inBuf);
					    ObjectInputStream object = new ObjectInputStream(input);
					    recvCont = (Content)object.readObject(); 
					    System.out.println("<Client> received packet back, the call id is: "+recvCont.callId+", and we want id: "+ID);
					} while( recvCont.callId!=ID);//the callID in inBuf is not the expected one
				    break;
				} catch(SocketTimeoutException stoe) {
					// timeout 
					// receive timeouts to decide that another machine is not responding.
					// not expected to implement retries of lost RPC messages -- in fact, this is discouraged.
					System.out.println("<Client> receive timeOut. tried sending to "+ destAddr);
					recvPkt = null;
					recvCont = null;
					
					break;
				} catch(Exception ioe) {
					// other error 
					//if IO error should retry to receive:
				}
			}
		rpcSocket.close();
		}
		catch(Exception ee)
		{
			ee.printStackTrace();
			System.out.println("<Client> procedureCall failed. Sending failure from me("+hello_user.serverID+"), to "+destAddr);
			recvCont = null;
		}
		Tuple tup = new Tuple(recvCont, returAddres);
		return tup;
	}
	

	public class Tuple
	{
		public final Content content;
		public final String address;
		public Tuple(Content con, String addr)
		{
			this.content = con;
			this.address = addr;
		}
		
	}
	

	
	
	
}
