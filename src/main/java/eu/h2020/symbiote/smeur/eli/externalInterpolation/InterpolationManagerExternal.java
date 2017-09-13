package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.InterpolationManager;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

/**
 * This class holds (will hold) the communication with the python implementation of the kriging based algorithm.
 * @author DuennebeilG
 *
 */
public class InterpolationManagerExternal implements InterpolationManager {

	ArrayBlockingQueue<InterpolationJob> jobQueue=null;
	
	@Override 
	public void init() {
		jobQueue=new ArrayBlockingQueue<InterpolationJob>(2, true);	
		// The capacity of 2 should be enough. 
		// Don't make it too huge so we can detect problems related to capacity 
		
		InterpolationRunner iRunner=new InterpolationRunner(jobQueue);
		Thread tRunner=new Thread(iRunner);
		tRunner.setDaemon(true);
		// TODO: Check what happens when a python job is still running when this service shuts down. 
		tRunner.setName("InterpolationRunner");

		tRunner.start();
		
	}
	
	@Override
	public void startInterpolation(RegionInformation regInfo, List<Observation> obs, InterpolationDoneHandler callback) {
		InterpolationJob ij=new InterpolationJob();
		ij.regInfo=regInfo;
		ij.obs=obs;
		ij.callback=callback;
		
		try {
			jobQueue.put(ij);
		} catch (InterruptedException e) {
			// Should never happen in practice but we need to handle it anyway.
			e.printStackTrace();
		}
	}
	

	
	// Internal routines not part of the interface.
	
}
