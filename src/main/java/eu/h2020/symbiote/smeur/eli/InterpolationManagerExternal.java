package eu.h2020.symbiote.smeur.eli;

import java.util.List;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.InterpolationManager.InterpolationDoneHandler;

/**
 * This class holds (will hold) the communication with the python implementation of the kriging based algorithm.
 * @author DuennebeilG
 *
 */
public class InterpolationManagerExternal implements InterpolationManager {

	@Override
	public void startInterpolation(RegionInformation regInfo, List<Observation> obs, InterpolationDoneHandler callback) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public StreetSegmentList doInterpolation(RegionInformation regInfo, List<Observation> observations) 
	{
		return null;
	}

}
