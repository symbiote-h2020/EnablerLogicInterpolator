package eu.h2020.symbiote.smeur.eli;

import org.junit.Test;

import eu.h2020.symbiote.smeur.Point;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;

public class TestPersistenceFunctions {
	
	@Test
	public void test() {
		PersistenceManager pm=new PersistenceManager();
		
		pm.init();
		
		
		// Empty streetSegementList
		StreetSegmentList ssl=new StreetSegmentList(); 
		
		pm.persistStreetSegmentList("some id", ssl);
		
		StreetSegmentList sslRead=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslRead));
		
		
		// Empty segment
		StreetSegment theSegment=new StreetSegment();
		
		ssl.put("SegmentID", theSegment);

		
		pm.persistStreetSegmentList("some id", ssl);
		
		sslRead=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslRead));

		
		// Non-Empty segment

		theSegment.id="ID1";
		theSegment.segmentData=new Point[] {new Point(1.0, 2.0), new Point(3.0, 4.0)};
		theSegment.comment="This is all so stupid :-)";
		
		pm.persistStreetSegmentList("some id", ssl);
		
		sslRead=pm.retrieveStreetSegmentList("some id");
		
		assert(ssl.equals(sslRead));

		
	}
	
}
