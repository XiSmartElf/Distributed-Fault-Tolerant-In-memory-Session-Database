package project1b;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;

public class SimpleDB{

	AmazonSimpleDBClient db;
	CreateDomainRequest crrq;
	AWSCredentials cr;
	//GetAttributesRequest get;
	DeleteAttributesRequest del;
	PutAttributesRequest put;
	SelectRequest sel;
	String domainName;
	String itemName;

	public SimpleDB(String accessKey, String secretKey, String domainName){
		this.cr = new BasicAWSCredentials(accessKey, secretKey);
		this.db = new AmazonSimpleDBClient(cr);
		this.domainName = domainName;
		this.itemName = "Tuple";
		//rq = new CreateDomainRequest(domainName);
		//get = new GetAttributesRequest();
		//this.sel = new SelectRequest();
		//this.del = new DeleteAttributesRequest();
		//del.setDomainName(domainName);
		this.crrq = new CreateDomainRequest();
		this.crrq.setDomainName(domainName);
		db.createDomain(crrq);
	}

	public List<String> readDBViews(){
		SelectRequest rq = new SelectRequest("SELECT * FROM "+domainName);
		SelectResult result = db.select(rq);
		List<Item> items = result.getItems();
		List<String> a = new ArrayList<String>();
		for(Item i : items){
			for(Attribute at : i.getAttributes()){
				a.add(at.getValue());
			}
		}
		return a;
	}

	public boolean writeDBViews(Hashtable<String,StatusTuple> localViews){
		Set<String> keys = localViews.keySet();
		Iterator<String> it = keys.iterator();
		List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>();
		while(it.hasNext()){
			String key = it.next();
			StatusTuple tuple = localViews.get(key);
			String val = tuple.toString();
			ReplaceableAttribute attr = new ReplaceableAttribute(tuple.srvID,val,true);
			attrs.add(attr);
		}
		PutAttributesRequest putrq = new PutAttributesRequest(domainName,itemName,attrs);
		db.putAttributes(putrq);
		return true;
	}
}

