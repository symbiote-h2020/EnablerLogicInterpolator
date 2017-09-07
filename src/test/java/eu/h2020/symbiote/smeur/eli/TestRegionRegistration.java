package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.Point;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;

public class TestRegionRegistration {

	InterpolatorLogic il;
	EnablerLogic elMock;
	PersistenceManagerInterface pmMock; 
	
	@Before
	public void setUp() throws Exception {
		il=new InterpolatorLogic();
		elMock=mock(EnablerLogic.class);
		pmMock=mock(PersistenceManagerInterface.class);
		
		il.setPersistenceManager(pmMock);
		il.initialization(elMock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testErrorBehavior() {
		RegisterRegion ric=new RegisterRegion();
		RegisterRegionResponse ricr;
		
		ricr=il.registerRegion(ric);	// consumerID is null --> fail
		
		assertEquals(RegisterRegionResponse.StatusCode.ERROR, ricr.status);
		assertNotNull(ricr.explanation);
		verifyZeroInteractions(elMock);
		
		ric.regionID="SomeID";
		
		ricr=il.registerRegion(ric);	// StreetSegmentList is null --> fail
		assertEquals(RegisterRegionResponse.StatusCode.ERROR, ricr.status);
		assertNotNull(ricr.explanation);
		verifyZeroInteractions(elMock);


		StreetSegmentList ssl=new StreetSegmentList();
		ric.streetSegments=ssl;
		
		ricr=il.registerRegion(ric);	// StreetSegmentList is empty --> fail
		assertEquals(RegisterRegionResponse.StatusCode.ERROR, ricr.status);
		assertNotNull(ricr.explanation);		
		verifyZeroInteractions(elMock);
				
	}

	
	@Test
	public void testRegistrationProcess() {

		RegisterRegion ric=new RegisterRegion();
		RegisterRegionResponse ricr;
		
		
		ric.regionID="SomeID";
		ric.streetSegments=new StreetSegmentList();

		StreetSegment ss=new StreetSegment();
		ss.id="1";
		ss.segmentData=new Point[] {new Point(1.0, 2.0), new Point(5.0, 6.0)}; // Center=3.0, 4.0
		ric.streetSegments.put(ss.id, ss);


		// Expect...
		ArgumentCaptor<ResourceManagerTaskInfoRequest> resourceRequestCapture = ArgumentCaptor.forClass(ResourceManagerTaskInfoRequest.class);
		when(elMock.queryResourceManager(resourceRequestCapture.capture())).thenReturn(null);
		
		ArgumentCaptor<String> idCapture=ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<StreetSegmentList> sslCapture=ArgumentCaptor.forClass(StreetSegmentList.class);

		doNothing().when(pmMock).persistStreetSegmentList(idCapture.capture(), sslCapture.capture());
		
		
		// Here's the testee call!!!
		ricr=il.registerRegion(ric);
		
		
		// Check results
		// 1. A response should have been returned with status ok
		assertEquals(RegisterRegionResponse.StatusCode.SUCCESS, ricr.status);
		
		
		assertEquals(2, resourceRequestCapture.getAllValues().size());	// Expect two calls to get resources.
		
		// 2. A request for fixed resources should have been generated
		ResourceManagerTaskInfoRequest request=resourceRequestCapture.getAllValues().get(0);
		assertNotNull(request);
		
		assertEquals("SomeID:fixed", request.getTaskId());
		assertEquals("P0000-00-00T00:10:00", request.getCachingInterval());
		assertEquals(3.0, request.getCoreQueryRequest().getLocation_long(), 1E-3);
		assertEquals(4.0, request.getCoreQueryRequest().getLocation_lat(), 1E-3);
		assertEquals((Integer)314291, request.getCoreQueryRequest().getMax_distance());	// Should roughly be 628.5/2.0; calculated by a service in the internet. But note, that the internet servie will have used a more accurate algorithm.
		
		// 3. A request for mobile resources should have been generated.
		// TODO: How are fixed and mobile resources distinguished. 
		request=resourceRequestCapture.getAllValues().get(1);
		assertNotNull(request);
		
		assertEquals("SomeID:mobile", request.getTaskId());
		assertEquals("P0000-00-00T00:01:00", request.getCachingInterval());
		assertEquals(3.0, request.getCoreQueryRequest().getLocation_long(), 1E-3);
		assertEquals(4.0, request.getCoreQueryRequest().getLocation_lat(), 1E-3);
		assertEquals((Integer)314291, request.getCoreQueryRequest().getMax_distance());	// Should roughly be 628.5/2.0; calculated by a service in the internet. But note, that the internet servie will have used a more accurate algorithm.
		
		
		// 4. The StreetSegmentList should be stored through the Persistence Manager.
		verify(pmMock, times(1)).persistStreetSegmentList(anyString(), anyObject());	// Needs to be two times as Mockito seems to also count 
		
		List<String> capturedIDs=idCapture.getAllValues();
		assertEquals("SomeID", capturedIDs.get(0));
		assertEquals(ric.streetSegments, sslCapture.getAllValues().get(0));
	}
}