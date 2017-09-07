package eu.h2020.symbiote.smeur.eli;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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

public class PersistenceManagerImpl implements PersistenceManagerInterface {

	String databaseName="symbiote_eli_database";	// Note: Not final on purpose
	
	static final String sslCollectionName="StreetSegmentLists";
	MongoCollection<Document> collSSL;

	static final String interpolCollectionName="InterpolatedValues";
	MongoCollection<Document> collInterpol;
	
	static final String observationCollectionName="MeasurementValues";
	MongoCollection<Document> collObservations;
	
	@Override
	public void init() {
		
		MongoDatabase database;

		MongoClient mongo=connect();
		
		
		// Accessing the database
		database = mongo.getDatabase(databaseName);
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
		
		if (!allCollections.contains(observationCollectionName))
			database.createCollection(observationCollectionName);
		collObservations=database.getCollection(observationCollectionName);
		
		
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

		persistGeneric(sslID, ssl, this.collSSL, StreetSegmentListDocument.class);

	}

	@Override
	public StreetSegmentList retrieveStreetSegmentList(String sslID) {
		
		Object o=retrieveGeneric(sslID, collSSL, StreetSegmentListDocument.class);
		if (o==null)
			return null;
        StreetSegmentListDocument ssld=(StreetSegmentListDocument)o;
		return ssld.theList;
	}


	@Override
	public boolean ySSLIdExists(String sslID) {
		Bson filter=Filters.eq(sslID);
		
		long n=collSSL.count(filter);
		if (n>1)	// Just a safeguard in case mongoDB has some ugly surprises for us.
			throw new IllegalStateException("More than one document for id "+sslID+ ". This shouldn't happen");
		
		return n==1;
	}

	
	// Deal with interpolated values.
	private static class InterpolatedValuesDocument {
		public String _id;
		public StreetSegmentList theList;

		public InterpolatedValuesDocument() 
		{			
		}
		
		public InterpolatedValuesDocument(String _id, StreetSegmentList ssl) {
			this._id=_id;
			theList=ssl;
		}
		
	}
	
	
	/**
	 * 
	 */
	@Override
	public void persistInterpolatedValues(String sslID, StreetSegmentList ssl) {

		persistGeneric(sslID, ssl, this.collInterpol, InterpolatedValuesDocument.class);

	}

	/**
 	 * @param sslID	The registered id of the street network
	 * @return	The street segment list with interpolated values or null, if it does not exist.
	 */
	@Override
	public StreetSegmentList retrieveInterpolatedValues(String sslID) {
		
		Object o=retrieveGeneric(sslID, collInterpol, InterpolatedValuesDocument.class);
		if (o==null)
			return null;
		InterpolatedValuesDocument interpold=(InterpolatedValuesDocument)o;
		return interpold.theList;		
	}

	
	private static class ObservationsDocument {
		public String _id;
		public List<Observation> theList;

		public ObservationsDocument() 
		{			
		}
		
		public ObservationsDocument(String _id, List<Observation> obsList) {
			this._id=_id;
			theList=obsList;
		}
		
	}
	
	
	@Override
	public void persistObservations(String sslID, List<Observation> observations) {
		
		persistGeneric(sslID, observations, this.collObservations, ObservationsDocument.class);
		
	}

	@Override
	public List<Observation> retrieveObservations(String sslID) {
		
		
		Object o=retrieveGeneric(sslID, collObservations, ObservationsDocument.class);
		if (o==null)
			return null;
		ObservationsDocument obsd=(ObservationsDocument)o;
		return obsd.theList;

	}
	
	
	
	
	/// Utils
	public void wipeoutDB() {
		MongoClient mongo=connect();
		
		MongoDatabase database = mongo.getDatabase(databaseName);
		database.drop();    // CU later :-)
	}
	
	public void useDebugDatabase() {
		databaseName="symbiote_eli_debugdb";
	}
	
	/// private stuff here
	private MongoClient connect() {
		// Creating a Mongo client
		MongoClient mongo = new MongoClient("localhost", 27017);

		// Creating Credentials
//		MongoCredential credential;
//		credential = MongoCredential.createCredential("sampleUser", "myDb", "password".toCharArray());
//		System.out.println("Connected to the database successfully");

		return mongo;
	}


	private Constructor<?> findCompatibleConstructorHelper(Class<?> claus, int i, Class<?> newClass, Class<?> ... args) {
		if (newClass==null)
			return null;
		Class<?>[] superArgs=(Class<?>[]) Arrays.copyOf(args, args.length);
		superArgs[i]=newClass;
		Constructor<?> constructor=findCompatibleConstructor(claus, superArgs);
		return constructor;
	}

	private Constructor<?> findCompatibleConstructor(Class<?> claus, Class<?> ... args) {
		Constructor<?> constructor=null;
		try {
			constructor=claus.getConstructor(args);
			return constructor;
		} catch (NoSuchMethodException | SecurityException e) {
		}
		
		for (int i=0; i<args.length; i++) {
			Class<?> superClass=args[i].getSuperclass();
			constructor=findCompatibleConstructorHelper(claus, i, superClass, args);
			if (constructor!=null)
				return constructor;
			
			for (Class<?> intf : args[i].getInterfaces()) {
				constructor=findCompatibleConstructorHelper(claus, i, intf, args);
				if (constructor!=null)
					return constructor;				
			}
		}
		return null;
	}
	
	private void persistGeneric(String id, Object content, MongoCollection<Document> coll, Class<?> documentClass) {
		try {
			Constructor<?> constructor=findCompatibleConstructor(documentClass, String.class, content.getClass());
			if (constructor==null) {
				throw new IllegalStateException("Totally confused");
			}
			Object doc=constructor.newInstance(id, content);

			ObjectMapper mapper = new ObjectMapper();
	        String json=null;

			json = mapper.writeValueAsString(doc);

			Document document=Document.parse(json);

			Bson filter=Filters.eq(id);
			coll.replaceOne(filter, document, (new UpdateOptions()).upsert(true));

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}

	private Object retrieveGeneric(String sslID, MongoCollection<Document> coll, Class<?> documentClass) {

		Bson filter=Filters.eq(sslID);
		FindIterable<Document> documentIter=coll.find(filter);
		
		Document document=documentIter.first();
		if (document==null)
			return null;
		
		String json=document.toJson();

        ObjectMapper mapper = new ObjectMapper();
        Object wrapperDoc=null;
        try {
        	wrapperDoc=mapper.readValue(json, documentClass);
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
		return wrapperDoc;
	}

	
}
