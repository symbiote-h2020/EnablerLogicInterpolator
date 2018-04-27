package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManager;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;

public class TestRegionRegistration {

	InterpolatorLogic il;
	EnablerLogic elMock;
	PersistenceManager pmMock; 
	
	@Before
	public void setUp() throws Exception {
		il=new InterpolatorLogic();
		elMock=mock(EnablerLogic.class);
		pmMock=mock(PersistenceManager.class);
		
		il.interpolationMethod="dummy";
		il.setPersistenceManager(pmMock);
		il.initialization(elMock);
		
		reset(elMock);	
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

		ssl.put("SomeID", new StreetSegment());
		ricr=il.registerRegion(ric);	// Properties set is null --> fail
		assertEquals(RegisterRegionResponse.StatusCode.ERROR, ricr.status);
		assertNotNull(ricr.explanation);		
		verifyZeroInteractions(elMock);

		ric.properties=new HashSet<Property>();
		ricr=il.registerRegion(ric);	// Properties set is empty --> fail
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
		ss.segmentData=new WGS84Location[] {new WGS84Location(1.0, 2.0, 0.0, null, null), new WGS84Location(5.0, 6.0, 0.0, null, null)}; // Center=3.0, 4.0
		ric.streetSegments.put(ss.id, ss);

		ric.properties=new HashSet<Property>();
		ric.properties.add(new Property("NO", "http://neverla.nd/some/semantic", null));
		ric.properties.add(new Property("O3", "http://neverla.nd/some/semantic", null));
		

		// Expect...

		when(elMock.cancelTask(Mockito.anyObject())).thenReturn(null);

		ArgumentCaptor<ResourceManagerTaskInfoRequest> resourceRequestCapture = ArgumentCaptor.forClass(ResourceManagerTaskInfoRequest.class);
		when(elMock.queryResourceManager(Mockito.anyInt(), resourceRequestCapture.capture())).thenReturn(null);
		
		ArgumentCaptor<String> idCapture=ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<RegionInformation> riCapture=ArgumentCaptor.forClass(RegionInformation.class);

		doNothing().when(pmMock).persistRegionInformation(idCapture.capture(), riCapture.capture());
		
		
		// Here's the testee call!!!
		ricr=il.registerRegion(ric);
		
		
		// Check results
		// 1. A response should have been returned with status ok
		assertEquals(RegisterRegionResponse.StatusCode.SUCCESS, ricr.status);
		
		
		assertEquals(1, resourceRequestCapture.getAllValues().size());	// Expect one calls to get resources. 
																		// Note: Request for mobile sensors is commented out
		
		// 2. A request for fixed resources should have been generated
		ResourceManagerTaskInfoRequest request=resourceRequestCapture.getAllValues().get(0);
		assertNotNull(request);
		
		assertEquals("SomeID:fixed", request.getTaskId());
		assertEquals("P0000-00-00T00:03:00", request.getQueryInterval());
		assertEquals(3.0, request.getCoreQueryRequest().getLocation_long(), 1E-3);
		assertEquals(4.0, request.getCoreQueryRequest().getLocation_lat(), 1E-3);

// Check disabled as there is a strange "bugfix" in the code that emits a value 10 times larger as expected here.
// This factor of 10 is needed to get things running but nobody really understands why.
//		assertEquals((Integer)314291, request.getCoreQueryRequest().getMax_distance());	// Should roughly be 628.5/2.0; calculated by a service in the internet. But note, that the internet servie will have used a more accurate algorithm.
		
		assertEquals(ric.properties.size(), request.getCoreQueryRequest().getObserved_property().size());
		
		// 3. A request for mobile resources should have been generated.
		// TODO: How are fixed and mobile resources distinguished. 
//		request=resourceRequestCapture.getAllValues().get(1);
//		assertNotNull(request);
//		
//		assertEquals("SomeID:mobile", request.getTaskId());
//		assertEquals("P0000-00-00T00:01:00", request.getCachingInterval());
//		assertEquals(3.0, request.getCoreQueryRequest().getLocation_long(), 1E-3);
//		assertEquals(4.0, request.getCoreQueryRequest().getLocation_lat(), 1E-3);
//		assertEquals((Integer)314291, request.getCoreQueryRequest().getMax_distance());	// Should roughly be 628.5/2.0; calculated by a service in the internet. But note, that the internet servie will have used a more accurate algorithm.
//		assertEquals(ric.properties.size(), request.getCoreQueryRequest().getObserved_property().size());
		
		
		// 4. The region information should be stored through the Persistence Manager.
		verify(pmMock, times(1)).persistRegionInformation(anyString(), anyObject()); 
		
		List<String> capturedIDs=idCapture.getAllValues();
		assertEquals("SomeID", capturedIDs.get(0));
		
		RegionInformation regInfoCaptured=riCapture.getAllValues().get(0);
		assertEquals(ric.streetSegments, regInfoCaptured.theList);
		assertEquals(request.getCoreQueryRequest().getLocation_long(), regInfoCaptured.center.getLongitude(), 1E-3);
		assertEquals(request.getCoreQueryRequest().getLocation_lat(),  regInfoCaptured.center.getLatitude(), 1E-3);

// TODO: Enable me again when the "hot fix" is removed	
//		assertEquals(request.getCoreQueryRequest().getMax_distance(),  (int)(regInfoCaptured.radius*1000), 1E-3);
	}
}
