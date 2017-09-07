package eu.h2020.symbiote.smeur.eli;

import java.util.List;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;

public interface InterpolationManagerInterface {
	
	public void startInterpolation(String regionID, PersistenceManagerInterface pm);
	
	public StreetSegmentList doInterpolation(StreetSegmentList ssl, List<Observation> observations);
}