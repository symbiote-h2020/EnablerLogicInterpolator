package eu.h2020.symbiote.smeur.eli;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;

public class PersistenceManager implements PersistenceManagerInterface {

	static String sslCollectionName="StreetSegmentLists";
	MongoCollection<Document> collSSL;

	static String interpolCollectionName="InterpolatedValues";
	MongoCollection<Document> collInterpol;
	
	@Override
	public void init() {
		// Creating a Mongo client
		MongoClient mongo = new MongoClient("localhost", 27017);

		// Creating Credentials
//		MongoCredential credential;
//		credential = MongoCredential.createCredential("sampleUser", "myDb", "password".toCharArray());
//		System.out.println("Connected to the database successfully");

		// Accessing the database
		MongoDatabase database = mongo.getDatabase("symbiote_eli_database");
//		System.out.println("Credentials ::" + credential);

		// Get all existing collection names and move them into a more convenient set.
		MongoIterable<String> collectionIterator=database.listCollectionNames();
		Set<String> allCollections=new HashSet<String>();
		for (String name : collectionIterator) {
			allCollections.add(name);
		}

		if (!allCollections.contains(sslCollectionName))
			database.createCollection(sslCollectionName);
		collSSL=database.getCollection(sslCollectionName);
		
		if (!allCollections.contains(interpolCollectionName))
			database.createCollection(interpolCollectionName);
		collInterpol=database.getCollection(interpolCollectionName);
		
		
	}


	private static class StreetSegmentListDocument {
		public String _id;
		public StreetSegmentList theList;

		public StreetSegmentListDocument() 
		{			
		}
		
		public StreetSegmentListDocument(String _id, StreetSegmentList ssl) {
			this._id=_id;
			theList=ssl;
		}
		
	}
	
	@Override
	public void persistStreetSegmentList(String sslID, StreetSegmentList ssl) {

		StreetSegmentListDocument ssld=new StreetSegmentListDocument(sslID, ssl);
		
        ObjectMapper mapper = new ObjectMapper();
        String json=null;
        try {
			json = mapper.writeValueAsString(ssld);
		} catch (JsonProcessingException e) {
			// Not really handled as any occurrence of this exception is due to an error during coding.
			e.printStackTrace();
		}

		
		Document document=Document.parse(json);
		Bson filter=Filters.eq(sslID);
		collSSL.replaceOne(filter, document, (new UpdateOptions()).upsert(true));
		//.insertOne(document);
	}

	@Override
	public StreetSegmentList retrieveStreetSegmentList(String sslID) {
		Bson filter=Filters.eq(sslID);
		FindIterable<Document> documentIter=collSSL.find(filter);
		
		Document document=documentIter.first();
		
		String json=document.toJson();

        ObjectMapper mapper = new ObjectMapper();
        StreetSegmentListDocument ssld=null;
        try {
			ssld=mapper.readValue(json, StreetSegmentListDocument.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ssld.theList;
	}

	@Override
	public boolean ySSLIdExists(String sslID) {
		Bson filter=Filters.eq(sslID);
		
		long n=collSSL.count(filter);
		if (n>1)	// JUst a safeguard in case mongoDB has some ugly surprises for us.
			throw new IllegalStateException("More than one document for id "+sslID+ ". This shouldn't happen");
		
		return n==1;
	}

	
	// Deal with interpolated values.
	/**
	 * 
	 */
	@Override
	public void persistInterpolatedValues(String sslID, StreetSegmentList ssl) {
		// TODO Auto-generated method stub
		
	}

	/**
 	 * @param sslID	The registered id of the street network
	 * @return	The street segment list with interpolated values or null, if it does not exist.
	 */
	@Override
	public StreetSegmentList retrieveInterpolatedValues(String sslID) {
		// TODO Auto-generated method stub
		return null;
	}

}
