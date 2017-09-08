package eu.h2020.symbiote.smeur.eli;


import java.util.List;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.StreetSegmentList;

/**
 * This defines the interface between ELI and MongoDB (or whatever we will implement in the future).
 * We split between interface and implementation to allow easy mocking.
 * @author DuennebeilG
 *
 */
public interface PersistenceManager {
	
	
	// Deal with street segments here (input data)
	/**
	 * Connect to the database here and other actions.
	 * 
	 */
	public void init();
	
	/**
	 * Store a region information for later use into the mongoDB.
	 */
	public void persistRegionInformation(String sslID, RegionInformation ssl);

	/**
	 * Retrieve a region information from the storage.
	 * @return
	 */
	RegionInformation retrieveRegionInformation(String sslID);
	
	/**
	 * Find out whether a certain sslID exists.
	 */
	public boolean ySSLIdExists(String sslID);

	
	
	// Deal with interpolated values here (output data)
	
	/**
	 * Store a bunch of interpolated values.
	 */
	public void persistInterpolatedValues(String sslID, StreetSegmentList ssl);
	
	/**
	 * Get them back again.
	 * @param sslID
	 * @return
	 */
	StreetSegmentList retrieveInterpolatedValues(String sslID);
	
	
	// Deal with measurements (observations) here
	/**
	 * Store measurements.
	 * @param sslID
	 */
	public void persistObservations(String sslID, List<Observation> observations);
	
	/**
	 * retrieve a set of observations.
	 * @param sslID
	 * @return
	 */
	public List<Observation> retrieveObservations(String sslID);
	
}
