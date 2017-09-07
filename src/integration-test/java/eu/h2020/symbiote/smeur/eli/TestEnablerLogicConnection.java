package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.messaging.consumers.SyncMessageFromEnablerLogicConsumer;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;

public class TestEnablerLogicConnection {

	/**
	 * These tests try to execute the EnablerLogicLayer up to a certain extend.
	 * Still not the complete RabbitMQ-Functionality but close to as we deal with the 
	 * text level (json encoded) layer.
	 * tests are simple, they mainly test whether a certain class fed in yields in an expected class given out.
	 * Detailed behavior due to certain input constellations is already exhaustedly tested in unit tests.
	 */
	
	
	InterpolatorLogic il;
	EnablerLogic el;
	PersistenceManagerInterface pmMock;
	
	
	SyncMessageFromEnablerLogicConsumer syncConsumer;
	
	@Before
	public void setUp() throws Exception {
		il=new InterpolatorLogic();
		
		el=new EnablerLogic(null, null);
		syncConsumer=new SyncMessageFromEnablerLogicConsumer();
		el.setSyncConsumer(syncConsumer);
		
		pmMock=mock(PersistenceManagerInterface.class);
		
		il.setPersistenceManager(pmMock);
		il.initialization(el);
	}

	@After
	public void tearDown() throws Exception {
	}

	
	Message encodeObject(Object o) throws JsonProcessingException, UnsupportedEncodingException {
		
		ObjectMapper mapper = new ObjectMapper();
        String json=null;

		json = mapper.writeValueAsString(o);

		byte[] encoded=json.getBytes("UTF-8");
		MessageProperties props=new MessageProperties();
		props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		props.setContentEncoding("UTF-8");
		
		Type inferredArgumentType=o.getClass();
		props.setInferredArgumentType(inferredArgumentType);
		
		
		Message msg=new Message(encoded, props);

		
		return msg;
	}
	
	
	@Test
	public void testRegisterRegion() throws JsonProcessingException, UnsupportedEncodingException {

		RegisterRegion ric=new RegisterRegion();

		Message msg=encodeObject(ric);
		
		try {
			Object o=syncConsumer.receivedSyncMessage(msg);
			assertTrue(o instanceof RegisterRegionResponse);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testQueryInterpolatedData() throws JsonProcessingException, UnsupportedEncodingException {

		QueryInterpolatedStreetSegmentList qil=new QueryInterpolatedStreetSegmentList();

		Message msg=encodeObject(qil);
		
		try {
			Object o=syncConsumer.receivedSyncMessage(msg);
			assertTrue(o instanceof QueryInterpolatedStreetSegmentListResponse);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testQueryPoiValues() throws JsonProcessingException, UnsupportedEncodingException {

		QueryPoiInterpolatedValues qil=new QueryPoiInterpolatedValues();

		Message msg=encodeObject(qil);
		
		try {
			Object o=syncConsumer.receivedSyncMessage(msg);
			assertTrue(o instanceof QueryPoiInterpolatedValuesResponse);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
