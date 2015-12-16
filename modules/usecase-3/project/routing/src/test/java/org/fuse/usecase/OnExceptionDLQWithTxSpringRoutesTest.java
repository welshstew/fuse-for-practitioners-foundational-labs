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
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.jms.ConnectionFactory;
import java.util.List;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentTransacted;

public class OnExceptionDLQWithTxSpringRoutesTest extends CamelSpringTestSupport {

    private final static String records = "Robocops,NA,true,Bill,Smith,100 N Park Ave.,Phoenix,AZ,85017,200-555-1000\n" +
            "MountainBikers,SA,true,George,Jungle,1101 Smith St.,Raleigh,NC,27519,600-555-7000\n" +
            "MicroservicesVision,WA,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,100-400-2000\n" +
            "Error,,,EU,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,900-000-4545";

    private String queueInputEndpoint = "amq-notx:usecase-input";
    private final String mockErrorEndpoint = "mock:error";

    @Test
    public void shouldGetMessageWithinDLQ() throws Exception {

        context.getRouteDefinitions().get(1).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("log-error-processor").after().to("mock:error");
            }
        });
        context.start();

        template.sendBody(queueInputEndpoint, records);

        // Set the expectations for the Mock endpoint
        MockEndpoint mock = getMockEndpoint(mockErrorEndpoint);
        // We should get an error message
        mock.expectedMessageCount(1);

        mock.assertIsSatisfied();

        assertEquals(1,mock.getExpectedCount());

        // We will verify that the header received correspond to our expectations
        List<Exchange> exchanges = mock.getExchanges();
        Message msg = exchanges.get(0).getIn();
        assertEquals("111",msg.getHeader("error-code"));
        assertEquals("No position 11 defined for the field: 19728, line: 1 must be specified",msg.getHeader("error-message"));

        // Check if a message has been published on the DLQ
        String result = (String) consumer.receiveBody("amq-notx:DLQ.usecase-input", 2000);
        assertEquals(records, result);
    }


    @Override protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/local/activemq-broker.xml","META-INF/spring/camel-context.xml");
    }
}
