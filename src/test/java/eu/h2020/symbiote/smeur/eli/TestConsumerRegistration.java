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
import org.mockito.Mockito;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.core.ci.SparqlQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.smeur.Point;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.RegisterInterpolationConsumer;

public class TestConsumerRegistration {

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
		RegisterInterpolationConsumer ric=new RegisterInterpolationConsumer();
		
		try {
			il.registerConsumer(ric);	// consumerID is null --> fail
			fail("Expected exception not thrown");
		} catch(IllegalArgumentException e) {
			// Pass
		}

		verifyZeroInteractions(elMock);
		
		ric.consumerID="SomeID";
		
		try {
			il.registerConsumer(ric);	// StreetSegmentList is null --> fail
			fail("Expected exception not thrown");
		} catch(IllegalArgumentException e) {
			// Pass
		}

		verifyZeroInteractions(elMock);


		StreetSegmentList ssl=new StreetSegmentList();
		ric.streetSegments=ssl;
		
		try {
			il.registerConsumer(ric);	// StreetSegmentList is empty --> fail
			fail("Expected exception not thrown");
		} catch(IllegalArgumentException e) {
			// Pass
		}
		
		verifyZeroInteractions(elMock);
				
	}

	
	@Test
	public void testRegistrationProcess() {

		RegisterInterpolationConsumer ric=new RegisterInterpolationConsumer();
		ric.consumerID="SomeID";
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
		il.registerConsumer(ric);
		
		
		// Check results
		// 1. A request for resources should have been generated
		ResourceManagerTaskInfoRequest request=resourceRequestCapture.getValue();
		assertNotNull(request);
		
		assertEquals(request.getCachingInterval(), "P0000-00-00T00:10:00");
		assertEquals(request.getCoreQueryRequest().getLocation_long(), 3.0, 1E-3);
		assertEquals(request.getCoreQueryRequest().getLocation_lat(), 4.0, 1E-3);
		assertEquals(request.getCoreQueryRequest().getMax_distance(), (Integer)314291);	// Should roughly be 628.5/2.0; calculated by a service in the internet. But note, that the internet servie will have used a more accurate algorithm.
		
		
		// 2. The StreetSegmentList should be stored through the Persistence Manager.
		verify(pmMock, times(1)).persistStreetSegmentList(anyString(), anyObject());	// Needs to be two times as Mockito seems to also count 
		
		List<String> capturedIDs=idCapture.getAllValues();
		assertEquals("SomeID", capturedIDs.get(0));
		assertEquals(ric.streetSegments, sslCapture.getAllValues().get(0));
	}
}
