package org.fuse.usecase;

import org.acme.Customer;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import javax.jms.ConnectionFactory;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentAutoAcknowledge;

public class OnExceptionDLQWithAUTOACKTest extends CamelTestSupport {

    private final static String records = "Robocops,NA,true,Bill,Smith,100 N Park Ave.,Phoenix,AZ,85017,200-555-1000\n" +
            "MountainBikers,SA,true,George,Jungle,1101 Smith St.,Raleigh,NC,27519,600-555-7000\n" +
            "MicroservicesVision,WA,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,100-400-2000\n" +
            "Error,,,EU,true,Fred,Quicksand,202 Barney Blvd.,Rock City,MI,19728,900-000-4545";

    private static final String componentName = "activemq";

    protected final String testingEndpoint = "activemq:input." + getClass().getName();

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                DataFormat bindyDF = new BindyCsvDataFormat(Customer.class);

                // we attempt to handle the exception and to send the message to our queue
                onException(Exception.class)
                    .handled(false)
                    .setHeader("error-code").constant(111)
                    .setHeader("error-message").simple("exception.message")
                    .to("direct:notify");

                from(testingEndpoint)
                   .split().tokenize("\n")
                     .log("Incoming JMS message ${body}")
                     .unmarshal(bindyDF)
                     .log("Message marshalled : ${body}")
                     .to("activemq:output");

                from("activemq:output")
                  .log("Message received ${body}");

                from("direct:notify")
                   .log(">> Direct endpoint called")
                   .to("activemq:error");
            }
        };
    }

    @Test
    public void shouldNOTGetMessageWithinDLQ() throws Exception {
        template.sendBody(testingEndpoint, records);

        // No message is moved to the DLQ
        Object dlqBody = consumer.receiveBody("activemq:ActiveMQ.DLQ", 2000);
        assertNull(dlqBody);

        String errorMsg = (String)consumer.receiveBody("activemq:error", 2000);
        assertStringContains(errorMsg,"Error");
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        // No redelivery
        ConnectionFactory connectionFactory = HelperTest.createConnectionFactory(null, 0);

        /* Without TxManager - Using AUTO_ACKNOWLEGEMENT MODE */
        camelContext.addComponent(componentName, jmsComponentAutoAcknowledge(connectionFactory));
        JmsComponent component = camelContext.getComponent(componentName, JmsComponent.class);
        return camelContext;
    }

}
