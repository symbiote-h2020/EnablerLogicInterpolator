package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManager;
import eu.h2020.symbiote.smeur.messages.PoIInformation;
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
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues(null);
		QueryPoiInterpolatedValuesResponse qpoir;
		
		qpoir=il.queryPoiValues(null);// argument is null --> fail
		
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);
		
		// Ok, ok, this is still sort of grey box testing. the sequence of the tests is in accordance with the internal sequence of error checks. 
		qpoir=il.queryPoiValues(qpoi);	// PoI list is null
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.ERROR, qpoir.status);
		assertNotNull(qpoir.explanation);
		verifyZeroInteractions(elMock);

	}

	/**
	 * Test behavior no fitting region.
	 */
	@Test
	public void testNoSuitableRegion() {
		HashMap<String, Location> thePoints=new HashMap<String, Location>();
		thePoints.put("SomePoint", new Location(20.0, 20.0, 0.0, null, null));

		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues(thePoints);
		QueryPoiInterpolatedValuesResponse qpoir;
		
		RegionInformation regInfo=new RegionInformation();
		regInfo.center=new Location(10.0, 10.0, 0.0, null, null);
		regInfo.radius=20.0;
		
		when(pmMock.getAllRegionIDs()).thenReturn(new HashSet<String>(Arrays.asList(new String[] {"Reg1"})));
		when(pmMock.retrieveRegionInformation("Reg1")).thenReturn(regInfo);
		
		// This is where the shit hits the fan....
		qpoir=il.queryPoiValues(qpoi);

		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.OK, qpoir.status);
		assertNull(qpoir.explanation);
		assertNotNull(qpoir.theData);
		
		PoIInformation poii=qpoir.theData.get("SomePoint");
		assertNotNull(poii);
		assertNotNull(poii.errorReason);
		
		verifyZeroInteractions(elMock);

	}

	/**
	 * Test behavior no interpolated values.
	 */
	@Test
	public void testNoInterpolatedValues() {
		
		HashMap<String, Location> thePoints = new HashMap<String, Location>();
		thePoints.put("SomePoint", new Location(20.0, 20.0, 0.0, null, null));
		
		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues(thePoints);
		QueryPoiInterpolatedValuesResponse qpoir;
		
		RegionInformation regInfo=new RegionInformation();
		regInfo.regionID="region";
		regInfo.center=new Location(19.9, 19.9, 0.0, null, null);
		regInfo.radius=20.0;
		
		when(pmMock.getAllRegionIDs()).thenReturn(new HashSet<String>(Arrays.asList(new String[] {"Reg1"})));
		when(pmMock.retrieveRegionInformation("Reg1")).thenReturn(regInfo);
		when(pmMock.retrieveInterpolatedValues("Reg1")).thenReturn(null);
		
		// Here is the call
		qpoir=il.queryPoiValues(qpoi);

		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.OK, qpoir.status);
		assertNull(qpoir.explanation);
		assertNotNull(qpoir.theData);
		
		PoIInformation poii=qpoir.theData.get("SomePoint");
		assertNotNull(poii);
		assertEquals("region", poii.regionID);
		assertNull(poii.interpolatedValues);
		assertNotNull(poii.errorReason);
		
		
		verifyZeroInteractions(elMock);

	}

	
	/**
	 * Test normal behavior.
	 */
	@Test
	public void testNormal() {
		HashMap<String, Location> thePoints = new HashMap<String, Location>();
		thePoints.put("PointID", new Location(9.9, 10.0, 0.0, null, null));

		QueryPoiInterpolatedValues qpoi=new QueryPoiInterpolatedValues(thePoints);
		QueryPoiInterpolatedValuesResponse qpoir;
		
		StreetSegmentList ssl=new StreetSegmentList();
		StreetSegment ss=new StreetSegment();

		ss.id="Who cares";
		ss.segmentData=new Location[] {new Location(10.0, 10.0, 0.0, null, null)};
		ss.exposure=new HashMap<String, ObservationValue>();			// We fill both, points and exposure here as we are lazy and reuse the same list for points and exposures.
		ss.exposure.put("NO", new ObservationValue("3.14", null, null));

		ssl.put("Who cares", ss);
		
		
		RegionInformation regInfo=new RegionInformation();
		regInfo.regionID="region";
		regInfo.center=new Location(9.9, 9.9, 0.0, null, null);
		regInfo.radius=20.0;
		regInfo.theList=ssl;
		
		when(pmMock.getAllRegionIDs()).thenReturn(new HashSet<String>(Arrays.asList(new String[] {"region"})));
		when(pmMock.retrieveRegionInformation("region")).thenReturn(regInfo);
		when(pmMock.retrieveInterpolatedValues("region")).thenReturn(ssl);
		
		
		
		// Fire away.
		qpoir=il.queryPoiValues(qpoi);

		
		// Result is as expected?
		assertEquals(QueryPoiInterpolatedValuesResponse.StatusCode.OK, qpoir.status);
		Map<String, PoIInformation> theData=qpoir.theData;
		assertNotNull(theData);
		
		assertEquals(1, theData.size());				// We queried one PoI, so the result should be 1
		assertTrue(theData.containsKey("PointID"));		// We queried it, so it should be there

		PoIInformation poii=theData.get("PointID");
		
		assertEquals("PointID", poii.poiID);
		assertNull(poii.errorReason);
		assertEquals("region", poii.regionID);
		
		Map<String, ObservationValue> exposures=poii.interpolatedValues;
		
		assertNotNull(exposures);
		assertEquals(1, exposures.size());				// Must change when the query defines the pollutants
		assertTrue(exposures.containsKey("NO"));		// Must change when the query defines the pollutants
		ObservationValue exposure=exposures.get("NO");			// Must change when the query defines the pollutants
		assertEquals("3.14", exposure.getValue());
		
		verifyZeroInteractions(elMock);
	}

}
