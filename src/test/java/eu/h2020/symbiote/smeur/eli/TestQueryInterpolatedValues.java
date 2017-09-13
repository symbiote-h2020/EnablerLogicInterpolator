package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;

public class TestQueryInterpolatedValues {

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
		
		reset(elMock);	// initialize has already made some calls that we don't want to know
	}

	@After
	public void tearDown() throws Exception {
	}

	
	/**
	 * Test some common error modes.
	 */
	@Test
	public void testErrorBehavior() {
		QueryInterpolatedStreetSegmentList qil=new QueryInterpolatedStreetSegmentList();
		QueryInterpolatedStreetSegmentListResponse qilr;
		
		qilr=il.queryInterpolatedData(null);	// argument is null --> fail
		
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.ERROR, qilr.status);
		assertNotNull(qilr.explanation);
		verifyZeroInteractions(elMock);
		
		qilr=il.queryInterpolatedData(qil);	// StreetSegmentID is null --> fail
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.ERROR, qilr.status);
		assertNotNull(qilr.explanation);
		verifyZeroInteractions(elMock);

		qil.sslID="";
		qilr=il.queryInterpolatedData(qil);	// StreetSegmentID is empty --> fail
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.ERROR, qilr.status);
		assertNotNull(qilr.explanation);
		verifyZeroInteractions(elMock);

	}


	/**
	 * Test behavior in case the requested ID has not been registered (or was lost somehow).
	 */
	@Test
	public void testQueryProcessFailUnknown() {

		QueryInterpolatedStreetSegmentList qil=new QueryInterpolatedStreetSegmentList();
		QueryInterpolatedStreetSegmentListResponse qilr;
		
		
		qil.sslID="SomeID";

		
		// Sequence of expected actions:
		// 1. See if the sslID is known.
		// 2. Query the interpolated values from mongo
		// 4. Give error back --> not existing

		// Expect...
		when(pmMock.yRegionExists("SomeID")).thenReturn(false);
		
		// Here's the testee call!!!
		qilr=il.queryInterpolatedData(qil);
		
		
		// Check results
		// 1. A response should have been returned with status UNKNOWN
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.UNKNOWN_SSLID, qilr.status);
	}


	/**
	 * Test behavior in case everything is cool but there are simply no interpolated values available for whatever reason.
	 */
	@Test
	public void testQueryProcessFailNotAvailable() {

		QueryInterpolatedStreetSegmentList qil=new QueryInterpolatedStreetSegmentList();
		QueryInterpolatedStreetSegmentListResponse qilr;
		
		
		qil.sslID="SomeID";

		
		// Sequence of expected actions:
		// 1. See if the sslID is known.
		// 2. Query the interpolated values from mongo
		// 4. Give error back --> not existing

		// Expect...
		when(pmMock.yRegionExists("SomeID")).thenReturn(true);
		when(pmMock.retrieveInterpolatedValues("SomeID")).thenReturn(null);
		
		// Here's the testee call!!!
		qilr=il.queryInterpolatedData(qil);
		
		
		// Check results
		// 1. A response should have been returned with status UNKNOWN
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.TRY_LATER, qilr.status);
	}

	
	
	/**
	 * Test behavior in case everything is cool but there are simply no interpolated values available for whatever reason.
	 */
	@Test
	public void testQueryProcessSuccess() {

		QueryInterpolatedStreetSegmentList qil=new QueryInterpolatedStreetSegmentList();
		QueryInterpolatedStreetSegmentListResponse qilr;
		
		
		qil.sslID="SomeID";

		
		// Sequence of expected actions:
		// 1. See if the sslID is known.
		// 2. Query the interpolated values from mongo
		// 4. Give error back --> not existing

		// Expect...
		when(pmMock.yRegionExists("SomeID")).thenReturn(true);
		when(pmMock.retrieveInterpolatedValues("SomeID")).thenReturn(new StreetSegmentList());
		
		// Here's the testee call!!!
		qilr=il.queryInterpolatedData(qil);
		
		
		// Check results
		// 1. A response should have been returned with status UNKNOWN
		assertEquals(QueryInterpolatedStreetSegmentListResponse.StatusCode.SUCCESS, qilr.status);
		assertNotNull(qilr.theList);
	}
}
