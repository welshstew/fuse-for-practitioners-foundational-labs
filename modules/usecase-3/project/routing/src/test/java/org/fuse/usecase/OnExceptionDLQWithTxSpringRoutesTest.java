package org.fuse.usecase;

import org.acme.Customer;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.jms.ConnectionFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentTransacted;

public class OnExceptionDLQWithTxSpringRoutesTest extends CamelSpringTestSupport {

    private final static String records =
            "Robocops,NA,true,Bill,Smith,100 N Park Ave.,Phoenix,AZ,85017,200-555-1000\n" +
                    "MountainBikers,SA,true,George,Jungle,1101 Smith St.,Raleigh,NC,27519,600-555-7000\n" +
                    "MicroservicesVision,WA,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,100-400-2000\n" +
                    "Error,,,EU,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,900-000-4545";

    private final static String[] jsonRecords = {
            "{\"company\":{\"name\":\"Robocops\",\"geo\":\"NA\",\"active\":true},\"contact\":{\"firstName\":\"Bill\",\"lastName\":\"Smith\",\"streetAddr\":\"100 N Park Ave.\",\"city\":\"Phoenix\",\"state\":\"AZ\",\"zip\":\"85017\",\"phone\":\"200-555-1000\"}}",
            "{\"company\":{\"name\":\"MountainBikers\",\"geo\":\"SA\",\"active\":true},\"contact\":{\"firstName\":\"George\",\"lastName\":\"Jungle\",\"streetAddr\":\"1101 Smith St.\",\"city\":\"Raleigh\",\"state\":\"NC\",\"zip\":\"27519\",\"phone\":\"600-555-7000\"}}",
            "{\"company\":{\"name\":\"MicroservicesVision\",\"geo\":\"WA\",\"active\":true},\"contact\":{\"firstName\":\"Fred\",\"lastName\":\"Quicksand\",\"streetAddr\":\"202 Barney Blvd.\",\"city\":\"Rock City\",\"state\":\"MI\",\"zip\":\"19728\",\"phone\":\"100-400-2000\"}}"
    };

    private String queueInputEndpoint = "amq-notx:usecase-input";
    private final String mockErrorEndpoint = "mock:error";
    private final String mockOutputEndpoint = "mock:output";

    @Test public void shouldGetMessageWithinDLQ() throws Exception {

/*      We get a ConcurrentException
       List<RouteDefinition> routes = Collections.synchronizedList(context.getRouteDefinitions());
        for (RouteDefinition route : routes) {
            if(route.getId().equals("error-queue")) {
                // We will extend the route of the error queue to add a mock endpoint
                route.adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        weaveById("log-error-processor").after().to("mock:error");
                    }
                });
            }

            if(route.getId().equals("queue-split-transform-queue")) {
                // We will append a processor after the endpoint saving the JMS messages within the queue
                // to check that we will save the messages
                route.adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        weaveById("output-queue-endpoint").after().to("mock:output");
                    }
                });
            }

        }*/

        // We will extend the route of the error queue to add a mock endpoint
        context.getRouteDefinition("error-queue").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override public void configure() throws Exception {
                weaveById("error-queue-endpoint").replace().to("mock:error");
            }
        });

        // We will append a processor after the endpoint saving the JMS messages within the queue
        // to check that we will save the messages
        context.getRouteDefinition("queue-split-transform-queue").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override public void configure() throws Exception {
                weaveById("output-queue-endpoint").after().to("mock:output");
            }
        });

        context.start();

        // Send the records (= what we have within the customers.csv file) to the input-usecase queue
        template.sendBody(queueInputEndpoint, records);

        // Set the expectations for the Mock endpoint
        MockEndpoint mockError = getMockEndpoint(mockErrorEndpoint);
        MockEndpoint mockOutput = getMockEndpoint(mockOutputEndpoint);
        // We should get an error message as the 4rd CSV record is erroneous and can't be processed by bindy.
        mockError.expectedMessageCount(1);

        mockError.assertIsSatisfied();
        mockOutput.assertIsSatisfied();

        assertEquals(1, mockError.getExpectedCount());

        // We verify that the headers received correspond to our expectations
        List<Exchange> exchanges = mockError.getExchanges();
        Message msg = exchanges.get(0).getIn();
        assertEquals("111", msg.getHeader("error-code"));
        assertEquals("No position 11 defined for the field: 19728, line: 1 must be specified",
                msg.getHeader("error-message"));

        // 3 messages should be received from the output-usecase queue
        exchanges = mockOutput.getExchanges();
        assertEquals(3, exchanges.size());
        int i = 0;
        for(Exchange exchange : exchanges) {
            byte[] body = (byte[]) exchange.getIn().getBody();
            assertEquals(jsonRecords[i],new String(body, StandardCharsets.UTF_8));
            i++;
        }

        // Check if a message has been published on the DLQ
        String result = (String) consumer.receiveBody("amq-notx:DLQ.usecase-input", 2000);
        assertEquals(records, result);
    }

    @Override protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/local/activemq-broker.xml",
                "META-INF/spring/camel-context.xml");
    }
}
