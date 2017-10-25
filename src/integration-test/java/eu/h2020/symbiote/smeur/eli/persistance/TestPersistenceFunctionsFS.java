package eu.h2020.symbiote.smeur.eli.persistance;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

public class TestPersistenceFunctionsFS {
	
	PersistenceManagerFS pm;
	
	@Before
	public void setup() {
		pm=new PersistenceManagerFS();
		
		pm.useDebugDatabase();
		pm.wipeoutDB();

		pm.init();
	}

	
	@AfterClass
	public static void finalCleanup() {
		PersistenceManagerMongo pm=new PersistenceManagerMongo();	// We need a private instance here as afterClass requires a static signature.
		
		pm.useDebugDatabase();
		pm.wipeoutDB();
	}
	
	@Test
	public void testRegionPersistance() {

		
		boolean yExists=pm.yRegionExists("some id");
		assertFalse(yExists);

		Set<String> allIDs=pm.getAllRegionIDs();
		assertNotNull(allIDs);
		assertTrue(allIDs.isEmpty());

		
		// Empty streetSegementList
		StreetSegmentList ssl=new StreetSegmentList();
		RegionInformation regInfo=new RegionInformation();
		regInfo.theList=ssl;
		
		pm.persistRegionInformation("some id", regInfo);
		
		RegionInformation regInfoReadBack=pm.retrieveRegionInformation("some id");
		
		assertEquals(regInfo, regInfoReadBack);

		
		yExists=pm.yRegionExists("some id");
		assertTrue(yExists);

		allIDs=pm.getAllRegionIDs();
		assertNotNull(allIDs);
		assertEquals(1, allIDs.size());
		assertTrue(allIDs.contains("some id"));


		// Empty segment
		StreetSegment theSegment=new StreetSegment();
		
		ssl.put("SegmentID", theSegment);

		
		pm.persistRegionInformation("some id", regInfo);
		
		regInfoReadBack=pm.retrieveRegionInformation("some id");
		
		assertEquals(regInfo, regInfoReadBack);

		
		// Non-Empty segment

		theSegment.id="ID1";
		theSegment.segmentData=new WGS84Location[] {new WGS84Location(1.0, 2.0, 0.0, null, null), new WGS84Location(3.0, 4.0, 0.0, null, null)};
		theSegment.comment="This is all so stupid :-)";
		
		pm.persistRegionInformation("some id", regInfo);
		
		regInfoReadBack=pm.retrieveRegionInformation("some id");
		
		assertEquals(regInfo, regInfoReadBack);
		
	}
	
	@Test
	public void testInterpolPersistance() {
		
		StreetSegmentList ssl=new StreetSegmentList(); 
		StreetSegment theSegment=new StreetSegment();
		theSegment.id="ID1";
		theSegment.comment="This is all so stupid :-)";
		theSegment.exposure=new HashMap<String, ObservationValue>();
		
		theSegment.exposure.put("NOx", new ObservationValue("3.14", null, null));
		
		ssl.put("SegmentID", theSegment);
		
		pm.persistInterpolatedValues("anotherID", ssl);
		StreetSegmentList sslReadBack=pm.retrieveInterpolatedValues("nonexisting");
		
		assertNull(sslReadBack);

		pm.persistInterpolatedValues("anotherID", ssl);
		sslReadBack=pm.retrieveInterpolatedValues("anotherID");
		
		assertEquals(ssl, sslReadBack);

	}

	
	
	@Test
	public void testObservationPersistance() {
		
		String sBaseTimeForTest="2016-11-11T01:00:00Z";

		List<Observation> observations=new ArrayList<Observation>();
		
		Observation obs=new Observation("rID1", null, sBaseTimeForTest, sBaseTimeForTest, null);
		observations.add(obs);

		Observation obs1=new Observation("rID1", null, sBaseTimeForTest, sBaseTimeForTest, null);
		observations.add(obs1);

		pm.persistObservations("obsID", observations);
		List<Observation> obsReadBack=pm.retrieveObservations("funny id");
		
		assertNull(obsReadBack);

		obsReadBack=pm.retrieveObservations("obsID");
		
		assertEquals(observations, obsReadBack);

	}

}
