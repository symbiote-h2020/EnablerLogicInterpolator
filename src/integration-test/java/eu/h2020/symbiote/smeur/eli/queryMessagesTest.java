package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.messaging.properties.ExchangeProperties;
import eu.h2020.symbiote.messaging.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.messaging.properties.RoutingKeysProperties;
import eu.h2020.symbiote.smeur.eli.InterpolatorLogic;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;

@RunWith(SpringRunner.class)
@Import({RabbitManager.class})
public class queryMessagesTest {

	// This class provides configuration for the test spring environment. Note the @Configuration annotation
	@Configuration
	public static class RabbitConfig {
		@Bean
		public ConnectionFactory connectionFactory() {
			return new CachingConnectionFactory("localhost");
		}
		
		@Bean
		public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
			rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
			return rabbitTemplate;
		}
		
		@Bean
		public EnablerLogicProperties enablerLogicProperties(
										RabbitConnectionProperties rabbitConnection, 
										ExchangeProperties exchangeProperties,
										RoutingKeysProperties routingKeyProperties) 
		{
			return new EnablerLogicProperties(rabbitConnection, exchangeProperties, routingKeyProperties);
		}

		@Bean
		public RabbitConnectionProperties rabbitConnectionProperties() 
		{
			return new RabbitConnectionProperties();
		}
	}
	
	
	
	@Autowired
	private ConnectionFactory factory;	// We need this to manage the embedded rabbitMQ, not for the test logic itself.
	
	

	@Autowired
    private RabbitManager rabbitManager;

	@Autowired
	private EnablerLogicProperties enablerLogicProperties;
    
	// We'll use this as a mocking partner for the communication
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	
	
	// Test resource. We use this to communicate.
    private static EmbeddedRabbitMq rabbitMq;
	private static final int RABBIT_STARTING_TIMEOUT = 60_000;

	private static final String EXCHANGE_NAME = "exchangeName";
	private static final String RECEIVING_QUEUE_NAME = "queueName";
	private static final String RECEIVING_ROUTING_KEY = "receivingQueue";

    
	@BeforeClass
	public static void startEmbeddedRabbit() throws Exception {
		EmbeddedRabbitMqConfig.Builder cb = new EmbeddedRabbitMqConfig.Builder();
		
		cb.rabbitMqServerInitializationTimeoutInMillis(RABBIT_STARTING_TIMEOUT);
		
		EmbeddedRabbitMqConfig config=cb.build();
		
		cleanupVarDir(config);

		rabbitMq = new EmbeddedRabbitMq(config);
		try {
			rabbitMq.start();
		} catch(Throwable t) {
			fail("Shit");
		}

//		RabbitMqPlugins rabbitMqPlugins = new RabbitMqPlugins(config);
//		try {
//			rabbitMqPlugins.enable("rabbitmq_management");
//			rabbitMqPlugins.enable("rabbitmq_tracing");
//		} finally {
//			rabbitMq.stop();
//		}
		System.out.println("RabbitMQ started");
	}
	

	private static void cleanupVarDir(EmbeddedRabbitMqConfig config) throws IOException {
		File varDir = new File(config.getAppFolder(), "var");
		if(varDir.exists())
			FileUtils.cleanDirectory(varDir);
	}
    
    @AfterClass
    public static void stopEmbeddedRabbit() throws InterruptedException {
    		rabbitMq.stop();
    		Thread.sleep(10_000);
    }

	
    /**
     * Cleaning up after previous test and creation of new connection and channel to RabbitMQ.
     * @throws Exception
     */
	@Before
	public void initializeRabbitMQResources() throws Exception {
		Connection connection = factory.createConnection();
		Channel channel = connection.createChannel(false);
				
		cleanRabbitResources(channel);
		createRabbitResources(channel);
	}

    
	
	private void createRabbitResources(Channel channel) throws IOException {
		channel.exchangeDeclare(EXCHANGE_NAME, "topic", true, true, false, null);
		channel.queueDeclare(RECEIVING_QUEUE_NAME, true, false, false, null);
		channel.queueBind(RECEIVING_QUEUE_NAME, EXCHANGE_NAME, RECEIVING_ROUTING_KEY);
	}

	private void cleanRabbitResources(Channel channel) throws IOException {
		channel.queueDelete(RECEIVING_QUEUE_NAME);
		channel.exchangeDelete(EXCHANGE_NAME);
	}

	
	
	@Test
	public void test() {
		
		EnablerLogic enablerLogic=new EnablerLogic(rabbitManager, enablerLogicProperties);
		
		InterpolatorLogic il=new InterpolatorLogic();
		il.initialization(enablerLogic);
		
		rabbitManager.sendMessage(EXCHANGE_NAME, RECEIVING_ROUTING_KEY, "Some String");
		Message receivedMessage=rabbitTemplate.receive(RECEIVING_QUEUE_NAME);
		assertNotNull(receivedMessage);
	}

}
