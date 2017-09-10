package eu.h2020.symbiote.smeur.eli;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

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
import eu.h2020.symbiote.enablerlogic.rap.plugin.RapPlugin;
import eu.h2020.symbiote.enablerlogic.rap.plugin.ReadingResourceListener;
import eu.h2020.symbiote.enablerlogic.rap.plugin.WritingToResourceListener;
import eu.h2020.symbiote.cloud.model.CloudResourceParams;
import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.core.model.WGS84Location;
import eu.h2020.symbiote.core.model.resources.Actuator;
import eu.h2020.symbiote.core.model.resources.Capability;
import eu.h2020.symbiote.core.model.resources.Effect;
import eu.h2020.symbiote.core.model.resources.EnumRestriction;
import eu.h2020.symbiote.core.model.resources.FeatureOfInterest;
import eu.h2020.symbiote.core.model.resources.Parameter;
import eu.h2020.symbiote.core.model.resources.Service;
import eu.h2020.symbiote.core.model.resources.StationarySensor;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.smeur.eli.model.MessageRequest;
import eu.h2020.symbiote.smeur.eli.model.MessageResponse;

@Component
public class InterpolatorLogic implements ProcessingLogic {
    private static final Logger LOG = LoggerFactory.getLogger(InterpolatorLogic.class);
    
    private EnablerLogic enablerLogic;
    
    @Autowired
    private EnablerLogicProperties props;
    
    @Autowired
    private RegistrationHandlerClientService rhClientService;
    
    @Autowired
    private RapPlugin rapPlugin;

    @Override
    public void initialization(EnablerLogic enablerLogic) {
        this.enablerLogic = enablerLogic;

        registerResources();
        
        registerRapConsumers();
        
        //asyncCommunication();
        //syncCommunication();

        //queryFixedStations();
        //queryMobileStations();
    }

    private void registerRapConsumers() {
        rapPlugin.registerReadingResourceListener(new ReadingResourceListener() {
            
            @Override
            public List<Observation> readResourceHistory(String resourceId) {
                if("1000".equals(resourceId))
                    return new ArrayList<>(Arrays.asList(createObservation(resourceId), createObservation(resourceId)));

                return null;
            }
            
            @Override
            public List<Observation> readResource(String resourceId) {
                if("1000".equals(resourceId)) {
                    Observation o = createObservation(resourceId);
                    return new ArrayList<>(Arrays.asList(o));
                }
                    
                return null;
            }
        });
        
        rapPlugin.registerWritingToResourceListener(new WritingToResourceListener() {
            
            @Override
            public Result<Object> writeResource(String resourceId, List<InputParameter> parameters) {
                LOG.debug("writing to resource {} body:{}", resourceId, parameters);
                if("2000".equals(resourceId)) {
                    Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("light")).findFirst();
                    if(lightParameter.isPresent()) {
                        String value = lightParameter.get().getValue();
                        if("on".equals(value)) {
                            LOG.debug("Turning on light {}", resourceId);
                            return new Result<>(false, null, "Turning on light " + resourceId);
                        } else if("off".equals(value)) {
                            LOG.debug("Turning off light {}", resourceId);
                            return new Result<>(false, null, "Turning off light " + resourceId);
                        }
                    }
                } else if("3000".equals(resourceId)) {
                    Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("trasholdTemperature")).findFirst();
                    if(lightParameter.isPresent()) {
                        String value = lightParameter.get().getValue();
                        LOG.debug("Setting trashold on resource {} to {}", resourceId, value);
                        return new Result<>(false, null, "Setting trashold on resource " + resourceId + " to " + value);
                    }
                }
                return null;
            }
        });
    }
    
    public Observation createObservation(String sensorId) {        
        Location loc = new Location(15.9, 45.8, 145, "Spansko", "City of Zagreb");
        
        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        Date date = new Date();
        String timestamp = dateFormat.format(date);
        
        long ms = date.getTime() - 1000;
        date.setTime(ms);
        String samplet = dateFormat.format(date);
        
        eu.h2020.symbiote.cloud.model.data.observation.ObservationValue obsval = 
                new eu.h2020.symbiote.cloud.model.data.observation.ObservationValue(
                        "7", 
                        new Property("Temperature", "Air temperature"), 
                        new UnitOfMeasurement("C", "degree Celsius", ""));
        ArrayList<eu.h2020.symbiote.cloud.model.data.observation.ObservationValue> obsList = new ArrayList<>();
        obsList.add(obsval);
        
        Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);
        
        try {
            LOG.debug("Observation: \n{}", new ObjectMapper().writeValueAsString(obs));
        } catch (JsonProcessingException e) {
            LOG.error("Can not convert observation to JSON", e);
        }
        
        return obs;
    }


    private void registerResources() {
        List<CloudResource> cloudResources = new LinkedList<>();
        cloudResources.add(createSensorResource("1000"));
        cloudResources.add(createActuatorResource("2000"));
        cloudResources.add(createServiceResource("3000"));

        // waiting for registrationHandler to create exchange
        int i = 1;
        while(i < 10) {
            try {
                LOG.debug("Atempting to register resources count {}.", i);
                rhClientService.registerResources(cloudResources);
                LOG.debug("Resources registered");
                break;
            } catch (Exception e) {
                i++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

    private CloudResource createSensorResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");

        StationarySensor sensor = new StationarySensor();
        cloudResource.setResource(sensor);
        sensor.setLabels(Arrays.asList("termometer"));
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

        return cloudResource;
    }

    private CloudResource createActuatorResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");
        
        Actuator actuator = new Actuator();
        cloudResource.setResource(actuator);
        actuator.setLabels(Arrays.asList("lamp"));
        actuator.setComments(Arrays.asList("A comment"));
        actuator.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        actuator.setLocatedAt(new WGS84Location(2.349014, 48.864716, 15, 
                Arrays.asList("Paris"), 
                Arrays.asList("This is Paris")));
        
        Capability capability = new Capability();
        actuator.setCapabilities(Arrays.asList(capability));
        Effect effect = new Effect();
        capability.setEffects(Arrays.asList(effect));
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        effect.setActsOn(featureOfInterest);
        Parameter parameter = new Parameter();
        capability.setParameters(Arrays.asList(parameter));
        parameter.setMandatory(true);
        parameter.setName("light");
        EnumRestriction enumRestriction = new EnumRestriction();
        enumRestriction.setValues(Arrays.asList("on", "off"));
        parameter.setRestrictions(Arrays.asList(enumRestriction));

        return cloudResource;
    }
    
    private CloudResource createServiceResource(String internalId) {
        CloudResource cloudResource = new CloudResource();
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(props.getEnablerName());
        cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");
        
        Service service = new Service();
        cloudResource.setResource(service);
        service.setLabels(Arrays.asList("lamp"));
        service.setComments(Arrays.asList("A comment"));
        service.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");
        
        service.setName("Heat alarm");
        Parameter parameter = new Parameter();
        parameter.setMandatory(true);
        parameter.setName("trasholdTemperature");
        service.setParameters(Arrays.asList(parameter));

        return cloudResource;
    }


    
    private void asyncCommunication() {
        // asynchronous communications to another Enabler Logic component
        // register consumer for message type EnablerLogicDataAppearedMessage
        enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
            EnablerLogicDataAppearedMessage.class, 
            (m) -> LOG.info("Received from another EnablerLogic: {}", m));
        
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
        
        LOG.info("Received sync response: {}", response.getResponse());
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
            LOG.info("querying fixed resources: {}", new ObjectMapper().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            LOG.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
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
            LOG.info("querying mobile resources: {}", new ObjectMapper().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            LOG.error("Problem with deserializing ResourceManagerAcquisitionStartResponse", e);
        }
    }
}
