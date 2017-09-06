package eu.h2020.symbiote.smeur.eli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;

/**
 * Use this implementation to get some results without running the python interpolator.
 * It will fill in random numbers. Intended usage: Integration testing with a limit number of dependencies.
 * @author DuennebeilG
 *
 */
public class InterpolationManagerDummyInterpolation implements InterpolationManagerInterface {
	

	Random random;
	
	
	InterpolationManagerDummyInterpolation() {
		random=new Random();
	}
	
	private static StreetSegment copySS(StreetSegment ss) {
		StreetSegment newSegment=new StreetSegment();
		newSegment.id=ss.id;
		newSegment.comment=ss.comment;
//		newSegment.segmentData=Arrays.copyOf(ss.segmentData, ss.segmentData.length);
		
		return newSegment;
		
	}
	
	
	public void startInterpolation(String regionID, PersistenceManagerInterface pm) {
		StreetSegmentList ssl=pm.retrieveStreetSegmentList(regionID);
		if (ssl==null)
			return;	// Shouldn't happen.
		
		StreetSegmentList interpolated=doInterpolation(ssl, null);
		
		pm.persistInterpolatedValues(regionID, interpolated);
	}
	
	
	public StreetSegmentList doInterpolation(StreetSegmentList ssl, List<Observation> observations) 
	{
		StreetSegmentList result=new StreetSegmentList();
		
		for (Entry<String, StreetSegment> entry : ssl.entrySet()) {
			String ssID=entry.getKey();
			StreetSegment ss=entry.getValue();
			StreetSegment newSegment=copySS(ss);
			
			fillInterpolatedValues(newSegment);
			
			result.put(ssID, newSegment);
		}
		
		return result;
	}

	private void fillInterpolatedValues(StreetSegment ss) {
		if (ss.exposure==null) {
			ss.exposure=new HashMap<String, Double>();
		}
		ss.exposure.clear();
		
		ss.exposure.put("NO2", random.nextDouble()*100);
		
	}
	
	
//	class InterpolationWorker implements Callable<StreetSegmentList> {
//
//		private StreetSegmentList ssl;
//
//		public InterpolationWorker(StreetSegmentList ssl) {
//			this.ssl=ssl;
//		}
//		
//		@Override
//		public StreetSegmentList call() throws Exception {
//			return InterpolationManagerDummyInterpolation.this.doInterpolation(ssl, null);
//		}
//		
//	}
}
