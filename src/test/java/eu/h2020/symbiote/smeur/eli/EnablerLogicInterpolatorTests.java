package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTasksStatus;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.Point;


//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, 
//                properties = {"eureka.client.enabled=false", 
//                              "spring.sleuth.enabled=false"}
//)
public class EnablerLogicInterpolatorTests {

	@Test
	public void testQueryForSensors() {
		InterpolatorLogic il=new InterpolatorLogic();
		
		EnablerLogic elMock=(EnablerLogic) mock(EnablerLogic.class);
		
		ResourceManagerAcquisitionStartResponse mockedResponse=new ResourceManagerAcquisitionStartResponse();
		mockedResponse.setStatus(ResourceManagerTasksStatus.FAILED);
		
		ArgumentCaptor<ResourceManagerTaskInfoRequest> resourceRequestCapture = ArgumentCaptor.forClass(ResourceManagerTaskInfoRequest.class);
		when(elMock.queryResourceManager(resourceRequestCapture.capture())).thenReturn(null);
		
		// Excercise the call here
		il.queryFixedStations(elMock, new Point(1.0, 2.0), 3.0);
		
		// Check results
		ResourceManagerTaskInfoRequest request=resourceRequestCapture.getValue();
		assertNotNull(request);
		
		assertEquals(request.getCachingInterval(), "P0000-00-00T00:10:00");
		
	}

}