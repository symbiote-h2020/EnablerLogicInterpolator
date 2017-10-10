package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.InterpolatorLogic;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

/**
 * This class is responsible to read a queue with InterpolationJobs
 * and execute them sequentially.
 * @author DuennebeilG
 *
 */
public class InterpolationRunner implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
	private ArrayBlockingQueue<InterpolationJob> jobQueue;

	File workingDirBase=new File("InterpolatorWorkingDir");	// TODO: Not hardcoded. Set from the config file.

	
	public InterpolationRunner(ArrayBlockingQueue<InterpolationJob> jobQueue) {
		this.jobQueue=jobQueue;
	}

	@Override
	public void run() {

		try {
			cycle();
		} catch (Throwable t) {
			log.error("InterpolationThread died: ", t);	// Just to avoid that the thread dies without a trace. 
		}
		
	}

	private void cycle() {
		while (true) {
			InterpolationJob job=null;
			try {
				job=jobQueue.take();
			} catch (InterruptedException e) {
				// Dead code. Shouldn't happen unless we change the complete concept
				e.printStackTrace();
			}
			
			if (job==null)
				continue;
			
			File workingDir=prepareCall(job.regInfo, job.obs, new Date());
			
			File theExecutable=new File(workingDirBase, "fakeInterpolation.py");
			
			runInterpolator(theExecutable, workingDir);
			StreetSegmentList interpolated=retrieveResults(job.regInfo, workingDir);
			
			job.callback.OnInterpolationDone(job.regInfo, interpolated);
			
			cleanupWorkingDir();
		}
	}

	
	private void cleanupWorkingDir() {
		try {
			IOFileFilter ageFilter=FileFilterUtils.ageFileFilter(new Date());
			Collection<File> allOldFiles=FileUtils.listFilesAndDirs(workingDirBase, ageFilter, ageFilter);
			for (File f : allOldFiles) {
				if (f.equals(workingDirBase))
					continue;
				if (f.exists())	// It might have been deleted already with it's directory.
					FileUtils.forceDelete(f);				
			}
		} catch(IOException ioe) {
			log.error("Problems cleaning up the working directory", ioe);
		}
		
	}


	// OMG, is this code ugly (with all these inferred types)
	HashMap<String, HashMap<String, ObservationValue>> rawInterpolatedToObservations(HashMap<String,HashMap<String,ArrayList<Object>>> interpolatedRaw) {
		HashMap<String, HashMap<String, ObservationValue>> result=new HashMap<String, HashMap<String, ObservationValue>>();
		
		for (Entry<String, HashMap<String, ArrayList<Object>>> entry : interpolatedRaw.entrySet()) {
			String segmentID=entry.getKey();
			HashMap<String, ArrayList<Object>> values=entry.getValue();
			for (Entry<String, ArrayList<Object>> e2 : values.entrySet()) {
				String propCode=e2.getKey();
				ArrayList<?> valuePair=e2.getValue();
				Double value=(Double) valuePair.get(0);
				String uom=(String) valuePair.get(1);
				ObservationValue obsValue=new ObservationValue(value.toString(), new Property(propCode, null), new UnitOfMeasurement(uom, null, null));
				if (! result.containsKey(segmentID)) {
					result.put(segmentID, new HashMap<String, ObservationValue>());
				}
				result.get(segmentID).put(propCode, obsValue);
			}
		}
		
		return result;
	}
	
	
	StreetSegmentList retrieveResults(RegionInformation regInfo, File workingDir) {
		StreetSegmentList interpolated=new StreetSegmentList();

		File interpolatedFile=new File(workingDir, "output.json");	// TODO: Shouldn't be hard coded
		
		FileInputStream fis;
		try {
			fis = new FileInputStream(interpolatedFile);
			byte[] bContent=IOUtils.toByteArray(fis);
			String sContent=new String(bContent);	// Platform dependent on purpose!!

	        ObjectMapper mapper = new ObjectMapper();
	        HashMap<String, HashMap<String, ArrayList<Object>>> interpolatedRaw=null;
	        interpolatedRaw=mapper.readValue(sContent, new TypeReference<HashMap<String, Object>>(){});
	        
	        HashMap<String, HashMap<String, ObservationValue>> interpolatedObservations=rawInterpolatedToObservations(interpolatedRaw);
	        
	        for (Entry<String, StreetSegment> entry : regInfo.theList.entrySet()) {
	        	String ssID=entry.getKey();
	        	StreetSegment ss=entry.getValue();
	        	StreetSegment ssCopy=new StreetSegment();
	        	ssCopy.id=ss.id;
	        	ssCopy.comment=ss.comment;
	        	ss.exposure=interpolatedObservations.get(ssID);
	        	
	        	interpolated.put(ssID, ssCopy);
	        }
	        
	        System.out.println(interpolatedRaw);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return interpolated;
	}

	void runInterpolator(File theExecutable, File workingDir) {
		
		boolean yWindows=false;
		String osName=System.getProperty("os.name");
		if (osName.toUpperCase().contains("WINDOWS"))
			yWindows=true;
		
		
		List<String> cmdLineArgs=new ArrayList<String>();
		
		if (yWindows) {
			cmdLineArgs.add("cmd.exe");	// Under windows, python is run through the extension ".py" and cmd.exe
										// Under linux it's an executable (run through the shebang indication)
			cmdLineArgs.add("/c");
		}
		
//		cmdLineArgs.add("dir");
		cmdLineArgs.add(theExecutable.getAbsolutePath());
		cmdLineArgs.add("SSL.json");
		cmdLineArgs.add("OBS.json");
		cmdLineArgs.add("interpolated.json");

		CommandLine cmdLine=new CommandLine(cmdLineArgs.remove(0));
		for (String c : cmdLineArgs) {
			cmdLine.addArgument(c, true);
		}

		DefaultExecutor executor=new DefaultExecutor();
		executor.setWorkingDirectory(workingDir);
		executor.setExitValue(0);
		try {
			executor.execute(cmdLine);
		} catch (ExecuteException ee) {
			// On wrong exit code
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * This method creates the necessary infrastructure for an external call of the interpolator.
	 * In particular it creates the working directory and the input files. 
	 * @param regInfo
	 * @param obs
	 * @param dirDate	This parameter is used mainly for integration testing. Feed in "now" (new Date()) in standard operation.
	 * @return The working directory prepared
	 */
	File prepareCall(RegionInformation regInfo, List<Observation> obs, Date dirDate) {
		
		SimpleDateFormat sdf=new SimpleDateFormat();
		sdf.applyPattern("yy_MM_dd_HHmmss");
		String wds=sdf.format(dirDate);
		wds=regInfo.regionID+"_"+wds;

		File workingDir=new File(workingDirBase, wds);
		
		boolean yOk=workingDir.mkdir();
		
		ReducedStreetSegmentList reducedList=new ReducedStreetSegmentList();
		reducedList.addAll(regInfo.theList);
		
		ObjectMapper mapper = new ObjectMapper();
        String json=null;

		try {
			json = mapper.writeValueAsString(reducedList);
		} catch (JsonProcessingException e) {
			// Any problem here can only be due to coding problems (aka "developer too stupid error")
			// So we just trace the problem.
			// Nothing more we 
			e.printStackTrace();
			throw new IllegalStateException("Error encoding streetSegmentList", e);
		}

		try {
			FileWriter fw=new FileWriter(new File(workingDir, "segmentList"));
			fw.append(json);
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		try {
			json = mapper.writeValueAsString(obs);
		} catch (JsonProcessingException e) {
			// Any problem here can only be due to coding problems (aka "developer too stupid error")
			// So we just trace the problem.
			// Nothing more we 
			e.printStackTrace();
			throw new IllegalStateException("Error encoding streetSegmentList", e);
		}

		
		try {
			FileWriter fw=new FileWriter(new File(workingDir, "observations"));
			fw.append(json);
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return workingDir;
		
	}

	
}
