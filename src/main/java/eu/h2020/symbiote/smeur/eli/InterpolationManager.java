package eu.h2020.symbiote.smeur.eli;

import java.util.List;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;

/**
 * This class holds (will hold) the communication with the python implementation of the kriging based algorithm.
 * @author DuennebeilG
 *
 */
public class InterpolationManager implements InterpolationManagerInterface {

	@Override
	public void startInterpolation(String regionID, PersistenceManagerInterface pm) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public StreetSegmentList doInterpolation(StreetSegmentList ssl, List<Observation> observations) 
	{
		return null;
	}

}
