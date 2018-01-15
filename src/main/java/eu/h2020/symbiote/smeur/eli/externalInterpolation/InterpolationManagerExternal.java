package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.smeur.eli.InterpolationManager;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

/**
 * This class holds (will hold) the communication with the python implementation of the kriging based algorithm.
 * @author DuennebeilG
 *
 */
public class InterpolationManagerExternal implements InterpolationManager {

	
	private static final Logger log = LoggerFactory.getLogger(InterpolationManagerExternal.class);

	
	ArrayBlockingQueue<InterpolationJob> jobQueue=null;


	private String executableForInterpolation;

	
	
	public void setInterpolationExecutable(String executableForInterpolation) {
		this.executableForInterpolation=executableForInterpolation;
	}
	
	
	@Override 
	public void init() {
		jobQueue=new ArrayBlockingQueue<InterpolationJob>(2, true);	
		// The capacity of 2 should be enough. 
		// Don't make it too huge so we can detect problems related to capacity 
		
		InterpolationRunner iRunner=new InterpolationRunner(jobQueue, executableForInterpolation);
		Thread tRunner=new Thread(iRunner);
		tRunner.setDaemon(true);
		// TODO: Check what happens when a python job is still running when this service shuts down. 
		tRunner.setName("InterpolationRunner");

		tRunner.start();
		
	}
	
	@Override
	public void startInterpolation(RegionInformation regInfo, List<Observation> obs, InterpolationDoneHandler callback) {
		
		log.info("Queuing a new external interpolation job for {} ", regInfo.regionID);
		
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
