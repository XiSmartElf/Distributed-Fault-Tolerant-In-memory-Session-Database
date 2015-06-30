package project1b;

import java.io.Serializable;



/**
 * @object: status information of a server in VIEW
 * @method: toString when save in simpleDB
 * @method: parseToTuple when getting string data from simpleDB
 */
public class StatusTuple implements Serializable
{
	String srvID;
	View.ServerStatus status; 
	long timeStamp;

	public StatusTuple(String serverId, View.ServerStatus stat, long time)
	{
		srvID = serverId;
		status = stat;
		timeStamp = time;
	}

	public String toString()
	{
		return srvID+":::"+status+":::"+timeStamp;
	}
	public static StatusTuple parseToTuple(String simpleDBval)
	{
		String[] temp = simpleDBval.split(":::");
		StatusTuple st = new StatusTuple(temp[0], View.ServerStatus.valueOf(temp[1]), Long.parseLong(temp[2]));
		return st;
	}
}
