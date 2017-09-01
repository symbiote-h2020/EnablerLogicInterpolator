package eu.h2020.symbiote.smeur.eli;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

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
import eu.h2020.symbiote.smeur.Point;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.RegisterInterpolationConsumer;
import eu.h2020.symbiote.smeur.messages.RegisterInterpolationConsumerResponse;

@Component
public class InterpolatorLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
	
	private EnablerLogic enablerLogic;

	private PersistenceManagerInterface pm=null;
	

	// Getter/Setter
	/**
	 * This routine allows to inject a PersistenceManager into the logic.
	 * The intended usage is to be used during unit testing.
	 * If the Manager is not set here, the init phase will create a suitable default manager. 
	 * @param pmMock
	 */
	public void setPersistenceManager(PersistenceManagerInterface pm) {
		this.pm=pm;
	}

	
	// public interface routines
	

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;

		if (pm==null) // Might have been already injected
			this.pm=new PersistenceManager();
		

//		public String registerConsumer(RegisterInterpolationConsumer ric) {


//		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
//				RegisterInterpolationConsumer.class, 
//			    (m) -> registerConsumer(m));
		
		// Read persistent data from Mongo here
		
		
//		queryFixedStations(enablerLogic);
//		queryMobileStations(enablerLogic);
	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
		System.out.println("received new Observations:\n"+dataAppeared);
	}

	
	// Internal logic and helpers
	
	
	protected void queryFixedStations(EnablerLogic el, Point center, Double radius) {
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId("Vienna-Fixed");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval("P0000-00-00T00:10:00"); // 10 mins.
		// Although the sampling period is either 30 mins or 60 mins there is a transmit
									// delay.
									// If we miss one reading by just 1 second and we set the interval to 30 mins we
									// are always 29 mins and 59 late.
		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(center.lat);
		coreQueryRequest.setLocation_long(center.lon);
		coreQueryRequest.setMax_distance((int)(radius*1000)); // radius 10km
		coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = el.queryResourceManager(request);

		try {
			log.info("querying fixed resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	protected void queryMobileStations(EnablerLogic el) {
		
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId("Vienna-Mobile");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval("P0000-00-00T00:01:00"); // 1 min

		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(48.208174);
		coreQueryRequest.setLocation_long(16.373819);
		coreQueryRequest.setMax_distance(10_000); // radius 10km
		coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = el.queryResourceManager(request);

		try {
			log.info("querying mobile resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	private double distance(Point p1, Point p2) {	// "Flat earth" approximation. Approximation valid for "small" distances.
		
		double latitudeMean=Math.toRadians((p1.lat+p2.lat)/2.0);	// We need this to compensate geometrical shortening of distances in lon with increasing lat
		double correctionFactor=Math.cos(latitudeMean);
		
		double diffLat=Math.toRadians(p1.lat-p2.lat);
		double diffLon=Math.toRadians(p1.lon-p2.lon)*correctionFactor;
		
		double distanceInRadian=Math.sqrt(diffLat*diffLat+diffLon*diffLon);	// Radians or fractions of the earth radius
		double distanceInKm=6_371.009 * distanceInRadian; 
		
		return distanceInKm;
	}
	
	private Object[] calculateCenterAndRadius(StreetSegmentList streetSegments) {
		double minLat=Double.MAX_VALUE;
		double maxLat=Double.MIN_NORMAL;
		double minLon=Double.MAX_VALUE;
		double maxLon=Double.MIN_VALUE;

		Iterator<Entry<String, StreetSegment>> it=streetSegments.entrySet().iterator();
		while (it.hasNext()) {
			StreetSegment ss=it.next().getValue();
			for (Point p : ss.segmentData) {
				minLat=Math.min(minLat, p.lat);
				maxLat=Math.max(maxLat, p.lat);
				minLon=Math.min(minLon, p.lon);
				maxLon=Math.max(maxLon, p.lon);
			}
		}
		

		double centerLat=(minLat+maxLat)/2.0;
		double centerLon=(minLon+maxLon)/2.0;

		Point center=new Point(centerLon, centerLat);


		double maxRadius=0.0;
		
		it=streetSegments.entrySet().iterator();
		while (it.hasNext()) {
			StreetSegment ss=it.next().getValue();
			for (Point p : ss.segmentData) {
				double dist=distance(p, center);
				maxRadius=Math.max(maxRadius, dist);
			}
		}

		
		return new Object[] {center, maxRadius};
	}
	
	public RegisterInterpolationConsumerResponse registerConsumer(RegisterInterpolationConsumer ric) {
		
		RegisterInterpolationConsumerResponse result=new RegisterInterpolationConsumerResponse();
		result.status=RegisterInterpolationConsumerResponse.StatusCode.SUCCESS;
		try {
			String consumerID=ric.consumerID;
			StreetSegmentList ssl=ric.streetSegments;
			
			if (consumerID==null || consumerID.isEmpty())
				throw new IllegalArgumentException("ConsumerID may not be null or empty");
			
			if (ssl==null || ssl.isEmpty())
				throw new IllegalArgumentException("Street segment list may not be null and must hold at least one segment");
			
			Object[] c_and_r=calculateCenterAndRadius(ssl);
			
			
			queryFixedStations(enablerLogic, (Point)c_and_r[0], (Double)c_and_r[1]);
			
			pm.persistStreetSegmentList(consumerID, ssl);
		} catch(Throwable t) {
			log.error("Problems when registering a consumer:", t);
			result.status=RegisterInterpolationConsumerResponse.StatusCode.ERROR;
			result.explanation=t.getMessage();
		}
		return result;
	}

}
