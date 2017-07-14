package eu.h2020.symbiote.smeur.eli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.h2020.symbiote.core.ci.SparqlQueryOutputFormat;
import eu.h2020.symbiote.core.ci.SparqlQueryRequest;



/**
 * First version of the enabler logic (interpolation)
 * @author DuennebeilG
 *
 */
public class EnablerLogicInterpolator {

	
	private static String requestQueueName = "symbIoTe.resourceManager";
	private static String exchange="symbIoTe.resourceManager";
	
	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		// Q: Why did I use username and password?
		// A: For debugging. This way I can identify the connections and from that the associated objects like exchanges, queues, ...
		factory.setUsername("Enabler_Interpolator");
		factory.setPassword("Enabler_Interpolator");
		
	    Connection connection = factory.newConnection();
	    
		QueryFixedStations(connection);
		QueryMobileStations(connection);
		
		
		RegisterAndArmCallbackForObservations(connection);
		
    }

	private static void QueryFixedStations(Connection connection) throws IOException, InterruptedException {
		Channel channel = connection.createChannel();

	    String replyQueueName = channel.queueDeclare().getQueue();
	    

	    QueryTask query=new QueryTask();
	    
	    query.setTaskID("Vienna-Fixed");
	    query.setCount(1);
	    query.setInterval(3600); // 10 mins. 
	    						 // Although the sampling period is either 30 mins or 60 mins there is a transmit delay.
	    						 // If we miss one reading by just 1 second and we set the interval to 30 mins we are always 29 mins and 59 late.
	    query.setLocation("Center: 48°12'N, 16°22'O; radius 10km");
	    List<String> obsProps=new ArrayList<String>();
	    obsProps.add("NOx");
	    query.setObservedProperties(obsProps);
	    
	    List<QueryTask> allQueries=new ArrayList<QueryTask>();
	    allQueries.add(query);
	    
	    QueryForResources queries=new QueryForResources();
	    queries.setResources(allQueries);

	    
    	ObjectMapper mapper = new ObjectMapper();
    	String json=mapper.writeValueAsString(queries);

	    String response=call(replyQueueName, channel, json);
	    System.out.println("Response is "+response);
	    	    
	}

	private static void QueryMobileStations(Connection connection) throws IOException, InterruptedException {
		Channel channel = connection.createChannel();

	    String replyQueueName = channel.queueDeclare().getQueue();
	    

	    QueryTask query=new QueryTask();
	    
	    query.setTaskID("Vienna-Mobile");
	    query.setCount(1);
	    query.setInterval(360); // 1 min  
	    query.setLocation("Center: 48°12'N, 16°22'O; radius 10km");
	    List<String> obsProps=new ArrayList<String>();
	    obsProps.add("NOx");
	    query.setObservedProperties(obsProps);
	    
	    List<QueryTask> allQueries=new ArrayList<QueryTask>();
	    allQueries.add(query);
	    
	    QueryForResources queries=new QueryForResources();
	    queries.setResources(allQueries);

	    
    	ObjectMapper mapper = new ObjectMapper();
    	String json=mapper.writeValueAsString(queries);

	    String response=call(replyQueueName, channel, json);
	    System.out.println("Response is "+response);
	    	    
	}
	
	
	
    public static String call(String replyQueueName, Channel channel, String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish(exchange, "symbIoTe.resourceManager.startDataAcquisition", props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);

        channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (properties.getCorrelationId().equals(corrId)) {
                    response.offer(new String(body, "UTF-8"));
                }
            }
        });

        return response.take();
    }

	
	
	private static void RegisterAndArmCallbackForObservations(Connection connection) throws IOException {
		Channel channel = connection.createChannel();
		
		String exchange="symbIoTe.enablerLogic";
		String queueName="symbiote.EnablerLogicInterpolator";
		
	    channel.exchangeDeclare(exchange, "topic");
	    
	    channel.queueDeclare(queueName, false, true, true, null);	// And yes, I know this differs from the confluence. But does it make sense to have this queue durable? Anyway. It will be non-durable during debugging.
	    channel.queueBind(queueName, exchange, "symbIoTe.enablerLogic.dataAppeared");

	    channel.basicQos(1);

	    System.out.println("Waiting for observations to be sent");

	    Consumer consumer = new DefaultConsumer(channel) {
	        @Override
	        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
	            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
	                    .Builder()
	                    .correlationId(properties.getCorrelationId())
	                    .build();

	            try {
	                String message = new String(body,"UTF-8");
	                System.out.println("received new Observations:\n"+message);
	            }
	            catch (RuntimeException e){
	                System.out.println(" [.] " + e.toString());
	            }
	            finally {
	            }
	        }
	    };

	    channel.basicConsume(queueName, false, consumer);

	}

}
