package eu.h2020.symbiote.smeur.eli;

import java.util.List;
import java.util.Set;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;

public interface InterpolationManager {
	
	public void startInterpolation(RegionInformation regInfo, List<Observation> obs, InterpolationDoneHandler callback);
	
	public StreetSegmentList doInterpolation(RegionInformation regInfo, List<Observation> observations);
	
	
	public interface InterpolationDoneHandler {
		public void OnInterpolationDone(RegionInformation regInfo, StreetSegmentList interpolated);
	}
	
}
