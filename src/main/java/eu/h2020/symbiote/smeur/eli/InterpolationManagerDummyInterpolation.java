package eu.h2020.symbiote.smeur.eli;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;

/**
 * Use this implementation to get some results without running the python interpolator.
 * It will fill in random numbers. Intended usage: Integration testing with a limit number of dependencies.
 * @author DuennebeilG
 *
 */
public class InterpolationManagerDummyInterpolation implements InterpolationManager {
	

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
	
	
	@Override
	public void startInterpolation(RegionInformation regInfo, List<Observation> obs, InterpolationManager.InterpolationDoneHandler callback) {
		
		StreetSegmentList interpolated=doInterpolation(regInfo, obs);

		callback.OnInterpolationDone(regInfo, interpolated);
	}
	
	
	public StreetSegmentList doInterpolation(RegionInformation regInfo, List<Observation> observations) 
	{
		
		StreetSegmentList result=new StreetSegmentList();
		
		
		StreetSegmentList ssl=regInfo.theList;

		for (Entry<String, StreetSegment> entry : ssl.entrySet()) {
			String ssID=entry.getKey();
			StreetSegment ss=entry.getValue();
			StreetSegment newSegment=copySS(ss);
			
			for (Property prop : regInfo.properties) {
				fillInterpolatedValues(prop, newSegment);
			}
			
			result.put(ssID, newSegment);
		}
		
		return result;
	}

	private void fillInterpolatedValues(Property prop, StreetSegment ss) {
		if (ss.exposure==null) {
			ss.exposure=new HashMap<String, ObservationValue>();
		}
		ss.exposure.clear();
		
		ObservationValue obsValue=new ObservationValue(Double.toString(random.nextDouble()*100), null, null); 
		ss.exposure.put(prop.getLabel(), obsValue);		
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
