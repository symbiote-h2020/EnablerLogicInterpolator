package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;

public class TestQueryPoiValues {

	InterpolatorLogic il;
	EnablerLogic elMock;
	PersistenceManager pmMock; 
	
	@Before
	public void setUp() throws Exception {
		il=new InterpolatorLogic();
		elMock=mock(EnablerLogic.class);
		pmMock=mock(PersistenceManager.class);
		
		il.setPersistenceManager(pmMock);
		il.initialization(elMock);
		
		reset(elMock);
	}

	@After
	public void tearDown() throws Exception {
	}

	
	/**
	 * Test some common error modes.
	 */
	@Test
	public void testErrorBehavior() {
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues();
		QueryPoiInterpolatedValuesResponse qpoir;
		
		qpoir=il.queryPoiValues(null);// argument is null --> fail
		
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);
		
		qpoir=il.queryPoiValues(qpoi);	// StreetSegmentID is null --> fail
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);

		qpoi.regionID="";
		qpoir=il.queryPoiValues(qpoi);	// StreetSegmentID is empty --> fail
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);
		
		// Ok, ok, this is still sort of grey box testing. the sequence of the tests is in accordance with the internal sequence of error checks. 
		qpoi.regionID="someID";
		qpoir=il.queryPoiValues(qpoi);	// PoI list is null
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);

	}

	/**
	 * Test behavior region not known.
	 */
	@Test
	public void testRegionNotKnown() {
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues();
		QueryPoiInterpolatedValuesResponse qpoir;
		
		qpoi.regionID="someID";
		
		when(pmMock.ySSLIdExists("someID")).thenReturn(false);
		
		qpoir=il.queryPoiValues(qpoi);

		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);

	}

	/**
	 * Test behavior region not known.
	 */
	@Test
	public void testNoInterpolatedValues() {
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues();
		QueryPoiInterpolatedValuesResponse qpoir;
		
		qpoi.regionID="someID";
		qpoi.thePoints=new HashMap<String, Location>();
		qpoi.thePoints.put("PointID", new Location(11,11, 0.0, null, null));

		StreetSegmentList ssl=new StreetSegmentList();		// Maybe empty. We don't use the content in this text
		
		when(pmMock.ySSLIdExists("someID")).thenReturn(true);
		when(pmMock.retrieveStreetSegmentList("someID")).thenReturn(ssl);
		when(pmMock.retrieveInterpolatedValues("someID")).thenReturn(null);
		
		qpoir=il.queryPoiValues(qpoi);

		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.TRY_AGAIN, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);
	}

	
	/**
	 * Test normal behavior.
	 */
	@Test
	public void testNormal() {
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues();
		QueryPoiInterpolatedValuesResponse qpoir;
		
		qpoi.regionID="someID";
		qpoi.regionID="someID";
		qpoi.thePoints=new HashMap<String, Location>();
		qpoi.thePoints.put("PointID", new Location(11,11, 0.0, null, null));

		StreetSegmentList ssl=new StreetSegmentList();		// Maybe empty. We don't use the content in this text
		StreetSegment ss=new StreetSegment();

		ss.id="Who cares";
		ss.segmentData=new Location[] {new Location(10.0, 10.0, 0.0, null, null)};
		ss.exposure=new HashMap<String, ObservationValue>();			// We fill both, points and exposure here as we are lazy and reuse the same list for points and exposures.
		ss.exposure.put("NO", new ObservationValue("3.14", null, null));

		ssl.put("Who cares", ss);
		
		
		when(pmMock.ySSLIdExists("someID")).thenReturn(true);
		when(pmMock.retrieveStreetSegmentList("someID")).thenReturn(ssl);
		when(pmMock.retrieveInterpolatedValues("someID")).thenReturn(ssl);
		
		qpoir=il.queryPoiValues(qpoi);

		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.OK, qpoir.status);
		Map<String, Map<String, Double>> theData=qpoir.theData;
		assertNotNull(theData);
		
		assertEquals(1, theData.size());				// We queried one PoI, so the result should be 1
		assertTrue(theData.containsKey("PointID"));		// We queried it, so it should be there
		
		Map<String, Double> exposures=theData.get("PointID");
		
		assertNotNull(exposures);
		assertEquals(1, exposures.size());				// Must change when the query defines the pollutants
		assertTrue(exposures.containsKey("NO"));		// Must change when the query defines the pollutants
		Double exposure=exposures.get("NO");			// Must change when the query defines the pollutants
		assertEquals(3.14, exposure, 0.000001);
		
		verifyZeroInteractions(elMock);
	}

}
