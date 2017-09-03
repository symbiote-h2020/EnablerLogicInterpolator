package eu.h2020.symbiote.smeur.eli;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.cloud.model.CloudResourceParams;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.model.WGS84Location;
import eu.h2020.symbiote.core.model.resources.FeatureOfInterest;
import eu.h2020.symbiote.core.model.resources.StationarySensor;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.smeur.eli.model.MessageRequest;
import eu.h2020.symbiote.smeur.eli.model.MessageResponse;

@Component
public class InterpolatorLogic implements ProcessingLogic {
    private static final Logger log = LoggerFactory.getLogger(InterpolatorLogic.class);
    
    private EnablerLogic enablerLogic;
    
    @Autowired
    private EnablerLogicProperties props;
    
    @Autowired
    private RegistrationHandlerClientService rhClientService;

    @Override
    public void initialization(EnablerLogic enablerLogic) {
        this.enablerLogic = enablerLogic;

        registerResources();
        
        //asyncCommunication();
        //syncCommunication();

        //queryFixedStations();
        //queryMobileStations();
    }

    private void registerResources() {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId("1600");
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");

        StationarySensor sensor = new StationarySensor();
        cloudResource.setResource(sensor);
        sensor.setLabels(Arrays.asList("lamp"));
        sensor.setComments(Arrays.asList("A comment"));
        sensor.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        sensor.setLocatedAt(new WGS84Location(2.349014, 48.864716, 15, 
                Arrays.asList("Paris"), 
                Arrays.asList("This is Paris")));
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        sensor.setFeatureOfInterest(featureOfInterest);
        featureOfInterest.setLabels(Arrays.asList("Room1"));
        featureOfInterest.setComments(Arrays.asList("This is room 1"));
        featureOfInterest.setHasProperty(Arrays.asList("temperature"));
        sensor.setObservesProperty(Arrays.asList("temperature,humidity".split(",")));
        
        CloudResourceParams cloudResourceParams = new CloudResourceParams();
        cloudResource.setParams(cloudResourceParams);
        cloudResourceParams.setType("Type of device, used in monitoring");

        rhClientService.registerResource(cloudResource);
    }

    private void asyncCommunication() {
        // asynchronous communications to another Enabler Logic component
        // register consumer for message type EnablerLogicDataAppearedMessage
        enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
            EnablerLogicDataAppearedMessage.class, 
            (m) -> log.info("Received from another EnablerLogic: {}", m));
        
        // send myself async message
        enablerLogic.sendAsyncMessageToEnablerLogic("EnablerLogicInterpolator", new EnablerLogicDataAppearedMessage());
    }
    
    private void syncCommunication() {
        // synchronous communication to another Enabler Logic component
        // register function for synchronous communication
        enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
            MessageRequest.class, 
            (m) -> new MessageResponse("response: " + m.getRequest()));
        
        // send myself sync message
        MessageResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
            "EnablerLogicInterpolator",
            new MessageRequest("request"),
            MessageResponse.class);
        
        log.info("Received sync response: {}", response.getResponse());
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
        request.setCachingInterval("P0000-00-00T00:10:00"); // 10 mins.
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
        request.setCachingInterval("P0000-00-00T00:01:00"); // 1 min

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
