package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.smeur.Point;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;

public class TestPersistenceFunctions {
	
	PersistenceManager pm;
	
	@Before
	public void setup() {
		pm=new PersistenceManager();
		
		pm.useDebugDatabase();
		pm.wipeoutDB();

		pm.init();
	}

	
	@AfterClass
	public static void finalCleanup() {
		PersistenceManager pm=new PersistenceManager();	// We need a private instance here as afterClass requires a static signature.
		
		pm.useDebugDatabase();
		pm.wipeoutDB();
	}
	
	@Test
	public void testSSLPersistance() {

		
		boolean yExists=pm.ySSLIdExists("some id");
		assertFalse(yExists);


		
		// Empty streetSegementList
		StreetSegmentList ssl=new StreetSegmentList(); 
		
		pm.persistStreetSegmentList("some id", ssl);
		
		StreetSegmentList sslReadBack=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslReadBack));

		
		yExists=pm.ySSLIdExists("some id");
		assertTrue(yExists);


		// Empty segment
		StreetSegment theSegment=new StreetSegment();
		
		ssl.put("SegmentID", theSegment);

		
		pm.persistStreetSegmentList("some id", ssl);
		
		sslReadBack=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslReadBack));

		
		// Non-Empty segment

		theSegment.id="ID1";
		theSegment.segmentData=new Point[] {new Point(1.0, 2.0), new Point(3.0, 4.0)};
		theSegment.comment="This is all so stupid :-)";
		
		pm.persistStreetSegmentList("some id", ssl);
		
		sslReadBack=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslReadBack));
		
	}
	
	@Test
	public void testInterpolPersistance() {
		
		StreetSegmentList ssl=new StreetSegmentList(); 
		StreetSegment theSegment=new StreetSegment();
		theSegment.id="ID1";
		theSegment.comment="This is all so stupid :-)";
		theSegment.exposure=new HashMap<String, Double>();
		
		theSegment.exposure.put("NOx", 3.14);
		
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
