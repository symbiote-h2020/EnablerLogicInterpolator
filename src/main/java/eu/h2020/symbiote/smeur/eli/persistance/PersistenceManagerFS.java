package eu.h2020.symbiote.smeur.eli.persistance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

public class PersistenceManagerFS implements PersistenceManager {

	private static final Logger log = LoggerFactory.getLogger(PersistenceManagerFS.class);
	
	String subDirName="symbiote_eli_database";	// Note: Not final on purpose
	
	static final String sslCollectionName="StreetSegmentLists";
	File sslPath;

	static final String interpolCollectionName="InterpolatedValues";
	File interpolPath;
	
	static final String observationCollectionName="MeasurementValues";
	File obsPath;

	
	private void mkDirIfNeeded(File path) {
		if (!path.exists()) {
			path.mkdir();
		}
		if (!path.isDirectory()) {
			throw new IllegalStateException("A file exists with the name for the persistance directory ("+path.getAbsolutePath()+")");
		}
		
	}
	
	@Override
	public void init() {
	

		File baseDir=new File(subDirName);
		mkDirIfNeeded(baseDir);

		sslPath=new File(baseDir, sslCollectionName);
		mkDirIfNeeded(sslPath);

		interpolPath=new File(baseDir, interpolCollectionName);
		mkDirIfNeeded(interpolPath);

		obsPath=new File(baseDir, observationCollectionName);
		mkDirIfNeeded(obsPath);
		
		
	}


	@SuppressWarnings("unused")
	private static class RegionInformationDocument {
		public String _id;
		public RegionInformation theRegion;

		public RegionInformationDocument() 
		{			
		}
		
		public RegionInformationDocument(String _id, RegionInformation theRegion) {
			this._id=_id;
			this.theRegion=theRegion;
		}
		
	}
	
	@Override
	public void persistRegionInformation(String sslID, RegionInformation ri) {

		persistGenericFS(sslID, ri, this.sslPath, RegionInformationDocument.class);

	}

	@Override
	public RegionInformation retrieveRegionInformation(String sslID) {
		
		Object o=retrieveGenericFS(sslID, sslPath, RegionInformationDocument.class);
		if (o==null)
			return null;
		RegionInformationDocument regd=(RegionInformationDocument)o;
		return regd.theRegion;
	}


	@Override
	public boolean yRegionExists(String sslID) {
		
		File filePath=new File(sslPath, sslID+".json");
		return filePath.exists();
		
	}

	
	@Override
	public Set<String> getAllRegionIDs() {
		
		IOFileFilter jsonFilter=new WildcardFileFilter("*.json");
		
		Collection<File> allFiles=FileUtils.listFiles(sslPath, jsonFilter , FileFilterUtils.falseFileFilter());
		HashSet<String> result=new HashSet<String>();
		
		for(File f : allFiles) {
			String name=f.getName();
			name=name.replace(".json", "");	// Only partially smart. What happens when someone creates an id that contains .json?
			result.add(name);
		}
		return result;
	}
	
	
	
	
	// Deal with interpolated values.
	@SuppressWarnings("unused")
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

		persistGenericFS(sslID, ssl, interpolPath, InterpolatedValuesDocument.class);

	}

	/**
 	 * @param sslID	The registered id of the street network
	 * @return	The street segment list with interpolated values or null, if it does not exist.
	 */
	@Override
	public StreetSegmentList retrieveInterpolatedValues(String sslID) {
		
		Object o=retrieveGenericFS(sslID, interpolPath, InterpolatedValuesDocument.class);
		if (o==null)
			return null;
		InterpolatedValuesDocument interpold=(InterpolatedValuesDocument)o;
		return interpold.theList;		
	}

	
	@SuppressWarnings("unused")
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
		
		persistGenericFS(sslID, observations, obsPath, ObservationsDocument.class);
		
	}

	@Override
	public List<Observation> retrieveObservations(String sslID) {
		
		
		Object o=retrieveGenericFS(sslID, obsPath, ObservationsDocument.class);
		if (o==null)
			return null;
		ObservationsDocument obsd=(ObservationsDocument)o;
		return obsd.theList;

	}
	
	
	
	
	/// Utils
	public void wipeoutDB() {
		
		File baseDir=new File(subDirName);
		
		try {
			if (baseDir.exists())
				FileUtils.forceDelete(baseDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void useDebugDatabase() {
		subDirName="symbiote_eli_debugdb";
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
	


	private void persistGenericFS(String id, Object content, File baseDir, Class<?> documentClass) {
		try {
			Constructor<?> constructor=findCompatibleConstructor(documentClass, String.class, content.getClass());
			if (constructor==null) {
				throw new IllegalStateException("Totally confused");
			}
			Object doc=constructor.newInstance(id, content);

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
//	        String json=null;

			File file=new File(baseDir, id+".json");
			
			mapper.writeValue(file, doc);
			
//			FileUtils.write(file, json, "UTF-8");
			
			

		} catch (SecurityException e) {
			// Note, if any of the below exceptions will occur we have fucked up during the coding phase.
			// So not much we can do to react here.
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
	}


	private Object retrieveGenericFS(String sslID, File path, Class<?> documentClass) {

		File file=new File(path, sslID+".json");
		String json=null;
		try {
			json = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		
		if (json==null || json.isEmpty())
			return null;
		
        ObjectMapper mapper = new ObjectMapper();
        Object wrapperDoc=null;
        try {
        	wrapperDoc=mapper.readValue(json, documentClass);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wrapperDoc;
	}



}
