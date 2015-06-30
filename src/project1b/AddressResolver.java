package project1b;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class AddressResolver {	
	/**
	 *  getIpAddress for running on AWS to obtain the EC2 instance's IP address that the code is running on
	 *  return the string version of the IPv4
	 *  return null if something goes wrong
	 *  
	 *  The ec2-metadata script actually issues an HTTP request to the EC2 infrastructure,
	 *  so it’s fairly expensive. Thus, you should call it only once and cache the result.
	 */
	
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
	
	/**
	 * Get IP address if tested on localhost
	 */
	public static String getIpLocalhost()
	{
	    String ip = null;
	    try {
	        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        while (interfaces.hasMoreElements()) {
	            NetworkInterface iface = interfaces.nextElement();
	            // filters out 127.0.0.1 and inactive interfaces
	            if (iface.isLoopback() || !iface.isUp())
	                continue;

	            Enumeration<InetAddress> addresses = iface.getInetAddresses();
	            while(addresses.hasMoreElements()) {
	                InetAddress addr = addresses.nextElement();
	                ip = addr.getHostAddress();
	                //System.out.println(iface.getDisplayName() + " " + ip);
	            }
	        }
	    } catch (SocketException e) {
	        throw new RuntimeException(e);
	    }
		return ip;
	}

}
