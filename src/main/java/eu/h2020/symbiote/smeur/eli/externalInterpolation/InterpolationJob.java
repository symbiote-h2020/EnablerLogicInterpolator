package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.util.List;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.eli.InterpolationManager.InterpolationDoneHandler;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

/**
 * This class holds all information needed to run an interpolation.
 * @author DuennebeilG
 *
 */
public class InterpolationJob {

	public RegionInformation regInfo;
	public List<Observation> obs;
	public InterpolationDoneHandler callback;

}
