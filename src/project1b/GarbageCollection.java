package project1b;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;


public class GarbageCollection extends Thread{
    Hashtable<String, SessionProperty> sessionStateTable = null;

	public GarbageCollection(Hashtable<String, SessionProperty> table)
	{
		this.sessionStateTable=table;
	}
	@Override
	public void run()
	{
		while(true)
		{
			List<String> deleteSet = new LinkedList<String>();
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
	
	public static Timestamp getCurrentTime()
	{ 
		//function used to get current time
		Calendar calendar = Calendar.getInstance();
		return new Timestamp(calendar.getTime().getTime());	
	}
	
}
