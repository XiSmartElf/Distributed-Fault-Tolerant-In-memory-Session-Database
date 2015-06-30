package project1b;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class View extends Thread {

	public static enum ServerStatus{UP, DOWN};

	private final int GOSSIP_SECS = 10 *1000; //10 secs interval
	public static final Hashtable<String,StatusTuple > myView = new Hashtable<String,StatusTuple >();
	public  String viewsIP = null;
	private RPC_Client rc= null;
    public SimpleDB db = null;
	
	
	/**
	 * @function: constructor to obtaining IP of this server
	 * @function: initialize view when start the server
	 * @param IP
	 */
	//this is supposed to be a singleton class/thread
	public View(String IP, RPC_Client RC)
	{
		this.viewsIP = IP;
		this.rc = RC;
		db = new SimpleDB("AKIAIGCBNBSRRYOVTARQ","8Vaz5uKuhRUZJtXnyJiOykMhL1FKoMEXQH4Y57gD","Views");
		updateSrvView(viewsIP, ServerStatus.UP, myView);
		db.writeDBViews(mergeView(getDBdata(), myView));
		
	}

	
	
	
	
	
	
	/**
	 * @function: call this whenever before using myView to update yourself(server)
	 * @param: serverId(local), status(UP status)
	 */
	public static void updateSrvView(String ip, ServerStatus status, Hashtable<String,StatusTuple> localView)
	{
		StatusTuple st = new StatusTuple(ip, status, System.currentTimeMillis());
		//put in view table
		localView.put(ip, st);
	}

	

	
	
	
	
	@Override
	public void run()
	{
		Random generator = new Random();
		while(true)
		{
			//*****  each gossip round *******
			//update yourself to up each round
			updateSrvView(viewsIP, ServerStatus.UP, myView);
			
			//exchange view for randomly selected gossip partner
			String randIp = (String) myView.keySet().toArray()[generator.nextInt(myView.keySet().size())];
			
			//if simpleDB is selected:
			if(randIp.equals(viewsIP))
			{
				//meger with simpleDB and write it back to simpleDB
				System.out.println("Select simplDB to exchange");
				db.writeDBViews(mergeView(getDBdata(), myView));
			}
			//if a server node is selected:
			else{
				System.out.println("Select "+randIp+" to exchange");
				ExchangeViews(randIp, rc);
			}
	
			//wait for next round
			try
			{
				Thread.sleep((GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS ));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	





	/**
	 * @function: request to exchange view between myView and a node's view
	 */
	private void ExchangeViews(String ipAddr, RPC_Client rc)
	{
		Content rvCont = rc.ExchangeViews(ipAddr, myView).content;
		//pckt lost and no response. procedure call timeout. then put it as down status
		if(rvCont ==null)
			updateSrvView(ipAddr, ServerStatus.DOWN, myView);
		else 
		{
			//if his view is returned back then merge it with mine
			mergeView(convertRecvView(rvCont.myView),myView);
		}
	}
	
	
	
	
	
	
	
	
	/**
	 *@function: read view from simpleDB
	 *@return: List of items in VIEW of simpleDB
	 */
	private ArrayList<StatusTuple> getDBdata()
	{
		ArrayList<String> dbData = (ArrayList<String>) db.readDBViews();
		ArrayList<StatusTuple> parsedDBview = new ArrayList<StatusTuple>();
		for(String each: dbData)
			parsedDBview.add(StatusTuple.parseToTuple(each));
		return parsedDBview;
	}
	
	
	
	


	/**
	 *@function: convert view from hashtable to arraylist for exchange view between servers
	 *@return: List
	 */
	public static ArrayList<StatusTuple> convertRecvView(Hashtable<String,StatusTuple> view)
	{
		ArrayList<StatusTuple> converted = new ArrayList<StatusTuple>();
		for(String key: view.keySet())
		{
			converted.add(view.get(key));
		}
		return converted;
	}
	
	
	
	
	
	
	
	
	
	/**
	 * 
	 * @function: to exchange between two servers
	 * @param: request server's view
	 * @return: merged view / updated myView
	 */
	public static Hashtable<String, StatusTuple> mergeView(ArrayList<StatusTuple> incomeView, Hashtable<String,StatusTuple> localView)
	{
		
		for(StatusTuple servTup: incomeView)
		{
			if(localView.containsKey(servTup.srvID))
			{
				StatusTuple st = localView.get(servTup.srvID);
				if(st.timeStamp < servTup.timeStamp)
					localView.put(servTup.srvID, servTup);	
			}
			else
			{
				localView.put(servTup.srvID, servTup);
			}
		}
		return localView;
	}
	
}
