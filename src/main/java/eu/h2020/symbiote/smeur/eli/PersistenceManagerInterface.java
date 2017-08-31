package eu.h2020.symbiote.smeur.eli;

import eu.h2020.symbiote.smeur.StreetSegmentList;

/**
 * This defines the interface between ELI and MongoDB (or whatever we will implement in the future).
 * We split between interface and implementation to allow easy mocking.
 * @author DuennebeilG
 *
 */
public interface PersistenceManagerInterface {
	
	
	/**
	 * Connect to the database here and other actions.
	 */
	public void init();
	
	/**
	 * Store a street segment list for later use into the mongoDB.
	 */
	public void persistStreetSegmentList(String sslID, StreetSegmentList ssl);

	/**
	 * Retrieve a SSL from the storage.
	 * @return
	 */
	StreetSegmentList retrieveStreetSegmentList(String sslID);
}
