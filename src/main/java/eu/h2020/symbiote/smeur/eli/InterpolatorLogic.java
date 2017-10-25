package eu.h2020.symbiote.smeur.eli;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourcesUpdated;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManager;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManagerFS;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManagerMongo;
import eu.h2020.symbiote.smeur.messages.DebugAction;
import eu.h2020.symbiote.smeur.messages.PoIInformation;
import eu.h2020.symbiote.smeur.messages.PushInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;


@Component
public class InterpolatorLogic implements ProcessingLogic, InterpolationManager.InterpolationDoneHandler {
	private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
	
	private EnablerLogic enablerLogic;

	private PersistenceManager pm=null;
	private InterpolationManager im=null;

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
	 * This routine allows to inject an interpolation manager.
	 * The intended usage is to inject mock objects for unit testing.
	 * If the manager is not set here, the init phase will create a suitable default.
	 * @param im
	 */
	public void setInterpolationManager(InterpolationManager im) {
		this.im=im;
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
	

	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;

		
		log.info("***************************************************************************");
		log.info("**  Enabler logic Interpolator coming up              *********************");
		log.info("***************************************************************************");
		
		
		if (pm==null) { // Might have been already injected
			this.pm=new PersistenceManagerFS();
		}
		pm.init();

		
		if (this.im==null) {	// Only if not injected.
			// TODO: Control the type created here by a setting in the config file.
			this.im=new InterpolationManagerDummyInterpolation();
			this.im.init();
		}

		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				RegisterRegion.class, 
			    (m) -> this.registerRegion(m));
		
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				QueryInterpolatedStreetSegmentList.class, 
			    (m) -> this.queryInterpolatedData(m));

		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
				QueryPoiInterpolatedValues.class, 
			    (m) -> this.queryPoiValues(m));

		enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
				DebugAction.class, 
			    (m) -> this.debugAction(m));

		
//		for (int i=0; i<2; i++) {
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		
//			DebugAction da=new DebugAction();
//			da.actionString="Yupp";
//			enablerLogic.sendSyncMessageToEnablerLogic("EnablerLogicInterpolator", da, String.class);
//		}

	}

	private void debugAction(DebugAction m) {
		System.out.println("Debug debug");
		return;
	}


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

		boolean knownID=pm.yRegionExists(sslID);
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

		
		RegionInformation regInfo=pm.retrieveRegionInformation(sslID);
		
		this.im.startInterpolation(regInfo, mergedObservations, this);
				
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

		log.info("Received a QueryPoiValues message");
		log.debug("Message is : {}", qpiv);

		
		QueryPoiInterpolatedValuesResponse result=new QueryPoiInterpolatedValuesResponse();
		
		try {
			if (qpiv==null) {
				throw new IllegalArgumentException("Argument must not be null");
			}

			Map<String, WGS84Location> poiList=qpiv.thePoints;
			if (poiList==null) {
				throw new IllegalArgumentException("List of PoI's must not be null");
			}


			result.theData=new HashMap<String, PoIInformation>();

			log.debug("Finding values for PoI's now");

			
			Set<String> allRegionIDs=pm.getAllRegionIDs();	// TODO: This routine can be placed outside so it's only called once 
			
			for (String regID : allRegionIDs) {

				log.debug("Testing region {}", regID);
				RegionInformation ri=pm.retrieveRegionInformation(regID);
				log.debug("ri is {}", ri);

				StreetSegmentList interpol=pm.retrieveInterpolatedValues(regID);

				Map<String, PoIInformation> resultsForThisRegion=findPoiValuesPerRegion(ri, interpol, poiList);

				result.theData.putAll(resultsForThisRegion);
				
			}


			Iterator<Entry<String, WGS84Location>> it=poiList.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, WGS84Location> e=it.next();
				String poiID=e.getKey();
				log.debug("No region found for poiID");
				PoIInformation pi=new PoIInformation();
				pi.errorReason="No suitable region found";
				result.theData.put(poiID, pi);
			}
						
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.OK;

			log.debug("Querying PoI's succeeded ok.");

		} catch (NoInterpolationYetException e) {
			log.error("No interpolated values available yet.", e);
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.TRY_AGAIN;
			result.explanation=e.getMessage();						
		} catch (Throwable t) {
			log.error("An exception occured during querying PoI's", t);
			result.status=QueryPoiInterpolatedValuesResponse.StatusCode.ERROR;
			result.explanation=t.toString();			
		}
		
		log.debug("Result is {}", result);
		
		return result;
	}

	

	public RegisterRegionResponse registerRegion(RegisterRegion ric) {

		log.info("Received a registerRegion message");
		log.info("Message is : {}", ric);
		
		RegisterRegionResponse result=new RegisterRegionResponse();
		result.status=RegisterRegionResponse.StatusCode.SUCCESS;
		try {
			String regionID=ric.regionID;
			StreetSegmentList ssl=ric.streetSegments;
			
			if (regionID==null || regionID.isEmpty())
				throw new IllegalArgumentException("ConsumerID may not be null or empty");
			
			if (ssl==null || ssl.isEmpty())
				throw new IllegalArgumentException("Street segment list may not be null and must hold at least one segment");
			
			Set<Property> properties=ric.properties;
			if (properties==null || properties.isEmpty())
				throw new IllegalArgumentException("List of properties must not be null or empty");
			
			
			Object[] c_and_r=calculateCenterAndRadius(ssl);
			WGS84Location center=(WGS84Location)c_and_r[0];
			Double radius=(Double)c_and_r[1];
			
			
			queryFixedStations(enablerLogic, regionID, center, radius, properties);
//			queryMobileStations(enablerLogic, regionID, center, radius, properties);
			
			RegionInformation regInfo=new RegionInformation();
			regInfo.regionID=regionID;
			regInfo.theList=ssl;
			regInfo.properties=properties;
			regInfo.center=center;
			regInfo.radius=radius;
			
			pm.persistRegionInformation(regionID, regInfo);

			// TODO: This behavior is just for testing.
			// It will provide dummy interpolated values without having measurement values available
			log.error("Warning: \"Auto\"-interpolation is switched on!!!!");
			im.startInterpolation(regInfo, null, this);
			
			log.info("Region registration passed ok");

		} catch(Throwable t) {
			log.error("Problems when registering a region:", t);
			result.status=RegisterRegionResponse.StatusCode.ERROR;
			result.explanation=t.getMessage();
		}
		
		log.info("Sending a response of\n"+result);
		
		return result;
	}
	

	
	public QueryInterpolatedStreetSegmentListResponse queryInterpolatedData(QueryInterpolatedStreetSegmentList request) {
		
		log.info("Received a query for interpolated values as:"+request);
		
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
			if (!this.pm.yRegionExists(sslID)) {
				response.status=QueryInterpolatedStreetSegmentListResponse.StatusCode.UNKNOWN_SSLID;
				response.explanation="The ID "+ sslID + " is not known to the interpolator. Has it been registered?";
				log.info("Replying with a response of "+response);				
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
		
		log.info("Replying with a response of "+response);
		
		return response;
	}

	
	
	// Handler called if an interpolation is done
	@Override
	public void OnInterpolationDone(RegionInformation regInfo, StreetSegmentList interpolated) {

		pm.persistInterpolatedValues(regInfo.regionID, interpolated);

		if (regInfo.yWantsPushing) {
			PushInterpolatedStreetSegmentList ipl=new PushInterpolatedStreetSegmentList();
			ipl.regionID=regInfo.regionID;
			ipl.theList=interpolated;
	
			
			this.enablerLogic.sendAsyncMessageToEnablerLogic("EnablerLogicGreenRouteController", ipl);
		}
		
	}


	
	
	// Internal logic and helpers
	
	private static String findNearestSegment(WGS84Location l, StreetSegmentList sslList) {
		
		if (sslList.size()<1) {
			throw new IllegalArgumentException("The list of street segments has zero size");	// THis should have been tested during registration. But better safe than sorry.
		}
		
		
		String bestID=null;
		double bestDistance=Double.MAX_VALUE;
		
		
		for (Entry<String, StreetSegment> entry : sslList.entrySet()) {
			String currentID=entry.getKey();
			StreetSegment currentSS=entry.getValue();
			WGS84Location currentCenter=getCenter(currentSS.segmentData);
			
			double currentDistance=distance(l, currentCenter);
			
			if (currentDistance<bestDistance) {
				bestID=currentID;
				bestDistance=currentDistance;
			}
		}
		return bestID;
	}

	private static WGS84Location getCenter(WGS84Location[] segmentData) {
		
		if (segmentData.length==0) {
			throw new IllegalArgumentException("Streetsegment has 0 points");
		}
	
		double latMean=0.0;
		double lonMean=0.0;
		
		for (WGS84Location p : segmentData) {
			latMean+=p.getLatitude();
			lonMean+=p.getLongitude();
		}
		
		
		latMean/=segmentData.length;
		lonMean/=segmentData.length;

		WGS84Location result=new WGS84Location(lonMean, latMean, 0.0, null, null);
			

		
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


	
	protected void queryFixedStations(EnablerLogic el, String consumerID, WGS84Location center, Double radius, Set<Property> props) {
		String samplingPeriod="P0000-00-00T00:10:00";
		// Although the sampling period is either 30 mins or 60 mins there is a transmit
									// delay.
									// If we miss one reading by just 1 second and we set the interval to 30 mins we
									// are always 29 mins and 59 late.

		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(center.getLatitude());
		coreQueryRequest.setLocation_long(center.getLongitude());
		coreQueryRequest.setMax_distance((int)(radius*1000)); // radius 10km

		List<String> propsAsString=new ArrayList<String>();
		for (Property p : props) 
			propsAsString.add(p.getName());
		coreQueryRequest.setObserved_property(propsAsString);

		ResourceManagerTaskInfoRequest request=new ResourceManagerTaskInfoRequest(
				consumerID+":fixed",	// TaskID 
				1,						// minNoResources	 
				coreQueryRequest,
				samplingPeriod,	// queryInterval 
                false,	// allowCaching 
                null,	// Caching interval
                false,	// Inform platformproxy 
                "interpolator",	// platform ID
                null); 
		
		ResourceManagerAcquisitionStartResponse response = el.queryResourceManager(request);

		try {
			log.info("querying fixed resources response: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	protected void queryMobileStations(EnablerLogic el, String consumerID, WGS84Location c, Double r, Set<Property> props) {
		
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest();
		request.setTaskId(consumerID+":mobile");
		request.setEnablerLogicName("interpolator");
		request.setMinNoResources(1);
		request.setCachingInterval("P0000-00-00T00:01:00"); // 1 min

		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setLocation_lat(c.getLatitude());
		coreQueryRequest.setLocation_long(c.getLongitude());
		coreQueryRequest.setMax_distance((int)(r*1000));
		
		List<String> propsAsString=new ArrayList<String>();
		for (Property p : props) 
			propsAsString.add(p.getName());
		coreQueryRequest.setObserved_property(propsAsString);
		
		request.setCoreQueryRequest(coreQueryRequest);
		ResourceManagerAcquisitionStartResponse response = el.queryResourceManager(request);

		try {
			log.info("querying mobile resources: {}", new ObjectMapper().writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
		}
	}

	private static double distance(WGS84Location p1, WGS84Location p2) {	// "Flat earth" approximation. Approximation valid for "small" distances.
		
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
			for (WGS84Location p : ss.segmentData) {
				minLat=Math.min(minLat, p.getLatitude());
				maxLat=Math.max(maxLat, p.getLatitude());
				minLon=Math.min(minLon, p.getLongitude());
				maxLon=Math.max(maxLon, p.getLongitude());
			}
		}
		

		double centerLat=(minLat+maxLat)/2.0;
		double centerLon=(minLon+maxLon)/2.0;

		WGS84Location center=new WGS84Location(centerLon, centerLat, 0.0, null, null);


		double maxRadius=0.0;
		
		it=streetSegments.entrySet().iterator();
		while (it.hasNext()) {
			StreetSegment ss=it.next().getValue();
			for (WGS84Location l : ss.segmentData) {
				double dist=distance(l, center);
				maxRadius=Math.max(maxRadius, dist);
			}
		}

		
		return new Object[] {center, maxRadius};
	}
	

	private PoIInformation queryPoiValuesForOnePoI(String pointID, WGS84Location poiLocation) {

		log.debug("Trying to find values for {}  @ {}", pointID, poiLocation);
		
		PoIInformation result=new PoIInformation();
		
		Set<String> allRegionIDs=pm.getAllRegionIDs();	// TODO: This routine can be placed outside so it's only called once 
		
		RegionInformation regInfo=null;
		for (String regID : allRegionIDs) {
			log.debug("Testing region {}", regID);
			RegionInformation ri=pm.retrieveRegionInformation(regID);
			
			log.debug("ri is {}", ri);
			double poiDistance=distance(ri.center, poiLocation);
			if (poiDistance<=ri.radius) {
				regInfo=ri;
				break;
			}			
		}
		
		if (regInfo==null) {	// Still no region? --> no suitable region available.
			log.debug("No region found");
			result.errorReason="No suitable region found";
			return result;
		}

		

		result.regionID=regInfo.regionID;
		
		StreetSegmentList sslList=regInfo.theList;
		StreetSegmentList interpol=pm.retrieveInterpolatedValues(regInfo.regionID);
		if (interpol==null) {
			result.errorReason="No interpolated values available for region";
			return result;			
		}

		
		result.poiID=pointID;

		log.debug("Searching region {} for a good street segment", regInfo.regionID);
		String nearestSegmentID=findNearestSegment(poiLocation, sslList);
		log.debug("nearest ID is {}", nearestSegmentID);
			
		StreetSegment nearestSS=interpol.get(nearestSegmentID);
		if (nearestSS==null) {
			result.errorReason="Internal error. A segment was in the ssl but not in the interpolated values.";
			return result;			
		}
			
		result.theSegment=nearestSS;
		result.interpolatedValues=nearestSS.exposure;

		log.debug("SSL Search for PoI: result={}", result);
		
		return result;
	}


	public void notEnoughResources(NotEnoughResourcesAvailable arg0) {
		log.info("notEnoughResources message received but not implemented");
	}


	public void resourcesUpdated(ResourcesUpdated arg0) {
		log.info("resourcesUpdated message received but not implemented");		
	}


	
	private Map<String, PoIInformation> findPoiValuesPerRegion(RegionInformation regInfo, StreetSegmentList interpol,
			Map<String, WGS84Location> poiList) {

		Map<String, PoIInformation> result=new HashMap<String, PoIInformation>();
		

		Iterator<Entry<String, WGS84Location>> it=poiList.entrySet().iterator();
		while (it.hasNext()) {

			Entry<String, WGS84Location> e=it.next();
			String poiID=e.getKey();
			WGS84Location poiLocation=e.getValue();
			
			double poiDistance=distance(regInfo.center, poiLocation);
			if (poiDistance>regInfo.radius)
				continue;


			PoIInformation poii=new PoIInformation();
			poii.regionID=regInfo.regionID;
			poii.poiID=poiID;
			
			if (interpol==null) {
				poii.errorReason="No interpolated values available for region";
			} else {

			
				log.debug("Searching region {} for a good street segment", regInfo.regionID);
				String nearestSegmentID=findNearestSegment(poiLocation, regInfo.theList);
				log.debug("nearest ID is {}", nearestSegmentID);
					
				StreetSegment nearestSS=interpol.get(nearestSegmentID);
				if (nearestSS==null) {
					poii.errorReason="Internal error. A segment was in the ssl but not in the interpolated values.";
				}
					
				poii.theSegment=nearestSS;
				poii.interpolatedValues=nearestSS.exposure;
			}
			result.put(poiID, poii);
			it.remove();
			
		}
		
		return result;
	}



	
}
