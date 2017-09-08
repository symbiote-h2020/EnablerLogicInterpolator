package eu.h2020.symbiote.smeur.eli;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;

@Component
public class InterpolatorLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
	
	private EnablerLogic enablerLogic;

	private PersistenceManager pm=null;
	private InterpolationManagerInterface im=null;

	private boolean yUseCutoff=true;	// Should only be disabled during unit testing. Or do you know another good reason? 
	

	// Getter/Setter
	/**
	 * This routine allows to inject a PersistenceManager into the logic.
	 * The intended usage is to be used during unit testing.
	 * If the Manager is not set here, the init phase will create a suitable default manager. 
	 * @param pmMock
	 */
	public void setPersistenceManager(PersistenceManager pm) {
		this.pm=pm;
	}

	/**
	 * Disable cutoff.
	 * Intended usage: unit testing.
	 * @param yUseCutoff
	 */
	public void useCutoff(boolean yUseCutOff) {
		this.yUseCutoff=yUseCutOff;
	}
	
	// public interface routines
	

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;

		if (pm==null) // Might have been already injected
			this.pm=new PersistenceManagerImpl();

		this.im=new InterpolationManagerDummyInterpolation();

		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				RegisterRegion.class, 
			    (m) -> this.registerRegion(m));
		
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				QueryInterpolatedStreetSegmentList.class, 
			    (m) -> this.queryInterpolatedData(m));

		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				QueryPoiInterpolatedValues.class, 
			    (m) -> this.queryPoiValues(m));

	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
		
		if (dataAppeared==null) {
			log.error("measurementReceived called with null as an argument. Cowardly refusing to work further with that.");
			return;	// Who does send such a mess? A pity we can't give immediate feedback here.
		}
		
		String taskID=dataAppeared.getTaskId();
		int colonIndex=taskID.lastIndexOf(":");
		
		if (colonIndex==-1) {
			log.error("Why do you send me ID's I never asked for ("+taskID+")?");
			return;	// Can't be one of my ID's, can it?
		}
		
		String sslID=taskID.substring(0, colonIndex);

		boolean knownID=pm.ySSLIdExists(sslID);
		if (!knownID)
			return;
		
		// Everything checked. Let's start the real work.
		List<Observation> theNewObs=dataAppeared.getObservations();
		List<Observation> theOldObs=pm.retrieveObservations(sslID);
		
		Instant now=Instant.now();
		Instant cutOffTime=now.minusSeconds(30*60);
		if (!this.yUseCutoff)
			cutOffTime=null;
		
		List<Observation> mergedObservations=mergeObservations(theNewObs, theOldObs, cutOffTime);	// Cutoff 30 mins
		
		
		this.pm.persistObservations(sslID, mergedObservations);
		
				
	}

	
	// We need a local exception to distinguish a special failure mode below.
	// Functionality is standard. We just need the special class to capture it
	// out of all other things thrown around.
	class NoInterpolationYetException extends IllegalArgumentException {

		private static final long serialVersionUID = 1L;

		public NoInterpolationYetException(String msg) {
			super(msg);
		}
		
	};
	
	public QueryPoiInterpolatedValuesResponse queryPoiValues(QueryPoiInterpolatedValues qpiv) {
		
		QueryPoiInterpolatedValuesResponse result=new QueryPoiInterpolatedValuesResponse();
		
		try {
			if (qpiv==null) {
				throw new IllegalArgumentException("Argument must not be null");
			}

			String regionID=qpiv.regionID;

			if (regionID==null || regionID.isEmpty()) {
				throw new IllegalArgumentException("regionID must not be null");
			}

			if (!pm.ySSLIdExists(regionID)) {
				throw new IllegalArgumentException("regionID \""+regionID+"\" is unknown");
			}

			Map<String, Location> poiList=qpiv.thePoints;
			if (poiList==null) {
				throw new IllegalArgumentException("List of PoI's must not be null");
			}
			
			StreetSegmentList sslList=pm.retrieveStreetSegmentList(regionID);
			StreetSegmentList interpol=pm.retrieveInterpolatedValues(regionID);
			if (interpol==null) {
				throw new NoInterpolationYetException("no interpolated values available");
			}
			
			result.theData=new HashMap<String, Map<String, Double>>();
			
			for (Entry<String, Location> entry : poiList.entrySet()) {
				String pointID=entry.getKey();
				
				Map<String, Double> exposures=new HashMap<String, Double>();
				
				Location l=entry.getValue();
				String nearestSegmentID=findNearestSegment(l, sslList);
				
				StreetSegment nearestSS=interpol.get(nearestSegmentID);
				if (nearestSS==null) {
					throw new IllegalStateException("Internal error: No interpolated segment found");
				}
				ObservationValue exposure=nearestSS.exposure.get("NO");
				String exposureValue=exposure.getValue();
				double dExpValue=Double.parseDouble(exposureValue);
				exposures.put("NO", dExpValue);
				
				result.theData.put(pointID, exposures);

			}

			
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.OK;
			
		// 3. For each point in the query:
		// 4.    Find the nearest segment for the point in the ssl
		// 5.    Find the same segment in the interpolated values
		// 6.    copy all pollutants (which are requested) to the result 

		} catch (NoInterpolationYetException e) {
			e.printStackTrace();
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.TRY_AGAIN;
			result.explanation=e.getMessage();						
		} catch (Throwable t) {
			t.printStackTrace();
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.ERROR;
			result.explanation=t.toString();			
		}
		return result;
	}

	
	public RegisterRegionResponse registerRegion(RegisterRegion ric) {
		
		RegisterRegionResponse result=new RegisterRegionResponse();
		result.status=RegisterRegionResponse.StatusCode.SUCCESS;
		try {
			String regionID=ric.regionID;
			StreetSegmentList ssl=ric.streetSegments;
			
			if (regionID==null || regionID.isEmpty())
				throw new IllegalArgumentException("ConsumerID may not be null or empty");
			
			if (ssl==null || ssl.isEmpty())
				throw new IllegalArgumentException("Street segment list may not be null and must hold at least one segment");
			
			Object[] c_and_r=calculateCenterAndRadius(ssl);
			
			
			queryFixedStations(enablerLogic, regionID, (Location)c_and_r[0], (Double)c_and_r[1]);
			queryMobileStations(enablerLogic, regionID, (Location)c_and_r[0], (Double)c_and_r[1]);
			
			pm.persistStreetSegmentList(regionID, ssl);

			// TODO: This behavior is just for testing.
			StreetSegmentList interpol=im.doInterpolation(ssl, null);
			pm.persistInterpolatedValues(regionID, interpol);

		} catch(Throwable t) {
			log.error("Problems when registering a consumer:", t);
			result.status=RegisterRegionResponse.StatusCode.ERROR;
			result.explanation=t.getMessage();
		}
		
		return result;
	}
	
	
	
	// Internal logic and helpers
	
	private static String findNearestSegment(Location l, StreetSegmentList sslList) {
		
		if (sslList.size()<1) {
			throw new IllegalArgumentException("The list of street segments has zero size");	// THis should have been tested during registration. But better safe than sorry.
		}
		
		
		String bestID=null;
		double bestDistance=Double.MAX_VALUE;
		
		
		for (Entry<String, StreetSegment> entry : sslList.entrySet()) {
			String currentID=entry.getKey();
			StreetSegment currentSS=entry.getValue();
			Location currentCenter=getCenter(currentSS.segmentData);
			
			double currentDistance=distance(l, currentCenter);
			
			if (currentDistance<bestDistance) {
				bestID=currentID;
				bestDistance=currentDistance;
			}
		}
		return bestID;
	}

	private static Location getCenter(Location[] segmentData) {
		
		if (segmentData.length==0) {
			throw new IllegalArgumentException("Streetsegment has 0 points");
		}
	
		double latMean=0.0;
		double lonMean=0.0;
		
		for (Location p : segmentData) {
			latMean+=p.getLatitude();
			lonMean+=p.getLongitude();
		}
		
		
		latMean/=segmentData.length;
		lonMean/=segmentData.length;

		Location result=new Location(lonMean, latMean, 0.0, null, null);
			

		
		return result;
	}

	protected List<Observation> mergeObservations( // Should be private but then it's not available for grey box unit testing
			List<Observation> theNewObs, 
			List<Observation> theOldObs, 
			Instant cutOffTime) {
		
		List<Observation> result;
		
		if (theOldObs==null && theNewObs==null)
			return null;
		
		if (theOldObs==null)
			result=new ArrayList<Observation>();
		else
			result=new ArrayList<Observation>(theOldObs);
		
		if (theNewObs!=null) {
			// Merge the new list in. 
			
			for (Observation obs : theNewObs) {
				if (!result.contains(obs)) {
					result.add(obs);
				}
			}
		}

		if (cutOffTime!=null) {
			Iterator<Observation> it=result.iterator();
			while (it.hasNext()) {
				Observation obs=it.next();
				String relevantTime=obs.getResultTime();
				if (relevantTime==null)
					relevantTime=obs.getSamplingTime();
				
				if (relevantTime==null) {	// Neither time set --> you are FIRED!!
					it.remove();
					continue;
				}
					
				Instant relevantInstant=Instant.parse(relevantTime);
				
				if (relevantInstant.isBefore(cutOffTime)) {
					it.remove();
					continue;
				}
			}
		}
		
		return result;
	}


	
	protected void queryFixedStations(EnablerLogic el, String consumerID, Location center, Double radius) {
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId(consumerID+":fixed");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval("P0000-00-00T00:10:00"); // 10 mins.
		// Although the sampling period is either 30 mins or 60 mins there is a transmit
									// delay.
									// If we miss one reading by just 1 second and we set the interval to 30 mins we
									// are always 29 mins and 59 late.
		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(center.getLatitude());
		coreQueryRequest.setLocation_long(center.getLongitude());
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

	protected void queryMobileStations(EnablerLogic el, String consumerID, Location c, Double r) {
		
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId(consumerID+":mobile");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval("P0000-00-00T00:01:00"); // 1 min

		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(c.getLatitude());
		coreQueryRequest.setLocation_long(c.getLongitude());
		coreQueryRequest.setMax_distance((int)(r*1000));
		coreQueryRequest.setObserved_property(Arrays.asList("NOx"));
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = el.queryResourceManager(request);

		try {
			log.info("querying mobile resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	private static double distance(Location p1, Location p2) {	// "Flat earth" approximation. Approximation valid for "small" distances.
		
		double latitudeMean=Math.toRadians((p1.getLatitude()+p2.getLatitude())/2.0);	// We need this to compensate geometrical shortening of distances in lon with increasing lat
		double correctionFactor=Math.cos(latitudeMean);
		
		double diffLat=Math.toRadians(p1.getLatitude()-p2.getLatitude());
		double diffLon=Math.toRadians(p1.getLongitude()-p2.getLongitude())*correctionFactor;
		
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
			for (Location p : ss.segmentData) {
				minLat=Math.min(minLat, p.getLatitude());
				maxLat=Math.max(maxLat, p.getLatitude());
				minLon=Math.min(minLon, p.getLongitude());
				maxLon=Math.max(maxLon, p.getLongitude());
			}
		}
		

		double centerLat=(minLat+maxLat)/2.0;
		double centerLon=(minLon+maxLon)/2.0;

		Location center=new Location(centerLon, centerLat, 0.0, null, null);


		double maxRadius=0.0;
		
		it=streetSegments.entrySet().iterator();
		while (it.hasNext()) {
			StreetSegment ss=it.next().getValue();
			for (Location l : ss.segmentData) {
				double dist=distance(l, center);
				maxRadius=Math.max(maxRadius, dist);
			}
		}

		
		return new Object[] {center, maxRadius};
	}
	
	public QueryInterpolatedStreetSegmentListResponse queryInterpolatedData(QueryInterpolatedStreetSegmentList request) {
		QueryInterpolatedStreetSegmentListResponse response=new QueryInterpolatedStreetSegmentListResponse();
		String sslID=null;
		
		try {
			if (request==null)
				throw new IllegalArgumentException("The request must not be null");
			
			sslID=request.sslID;
			if (sslID==null || sslID.isEmpty()) {
				throw new IllegalArgumentException("The sslID must not be null or empty");
			}
			
			// Check if the SSLID is known.
			if (!this.pm.ySSLIdExists(sslID)) {
				response.status=QueryInterpolatedStreetSegmentListResponse.StatusCode.UNKNOWN_SSLID;
				response.explanation="The ID "+ sslID + " is not known to the interpolator. Has it been registered?";
				return response;
			}
			
			StreetSegmentList interpol=this.pm.retrieveInterpolatedValues(sslID);
			response.theList=interpol;
			response.status=QueryInterpolatedStreetSegmentListResponse.StatusCode.SUCCESS;

			if (interpol==null) {
				response.status=QueryInterpolatedStreetSegmentListResponse.StatusCode.TRY_LATER;
			}
			
			
		} catch(Throwable t) {
			log.error("Exception during querying interpolated values:", t);
			response.status=QueryInterpolatedStreetSegmentListResponse.StatusCode.ERROR;
			response.explanation=t.getMessage();
		}
		
		return response;
	}

}
