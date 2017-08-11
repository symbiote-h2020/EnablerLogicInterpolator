package eu.h2020.symbiote.smeur.eli;

import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.ProcessingLogic;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;

@Component
public class InterpolatorLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
	
	private EnablerLogic enablerLogic;

	@Override
	public void init(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;
    
		queryFixedStations();
		queryMobileStations();
	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
		System.out.println("received new Observations:\n"+dataAppeared);
	}
	
	private void queryFixedStations() {
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId("Vienna-Fixed");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval_ms(3600L); // 10 mins.
		// Although the sampling period is either 30 mins or 60 mins there is a transmit
									// delay.
									// If we miss one reading by just 1 second and we set the interval to 30 mins we
									// are always 29 mins and 59 late.
		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(48.208174);
		coreQueryRequest.setLocation_long(16.373819);
		coreQueryRequest.setMax_distance(10_000); // radius 10km
		coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

		try {
			log.info("querying fixed resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	private void queryMobileStations() {
		
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId("Vienna-Mobile");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval_ms(360L); // 1 min

		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(48.208174);
		coreQueryRequest.setLocation_long(16.373819);
		coreQueryRequest.setMax_distance(10_000); // radius 10km
		coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

		try {
			log.info("querying mobile resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}
}
