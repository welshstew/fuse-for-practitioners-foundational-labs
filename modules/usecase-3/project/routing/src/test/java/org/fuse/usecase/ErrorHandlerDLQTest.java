package org.fuse.usecase;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import javax.jms.ConnectionFactory;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentTransacted;

public class ErrorHandlerDLQTest extends CamelTestSupport {

    public static class BadErrorHandler {
        @Handler
        public void onException(Exchange exchange, Exception exception) throws Exception {
            throw new RuntimeException("error in errorhandler");
        }
    }

    protected final String testingEndpoint = "activemq:test." + getClass().getName();

    protected boolean isHandleNew() {
        return true;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // we use DLC to handle the exception but if it throw a new exception
                // then the DLC handles that too (the transaction will always commit)
                errorHandler(deadLetterChannel("bean:" + BadErrorHandler.class.getName())
                        .deadLetterHandleNewException(isHandleNew())
                        .logNewException(true));

                from(testingEndpoint)
                        .log("Incoming JMS message ${body}")
                        .throwException(new RuntimeException("bad error"));
            }
        };
    }

    @Test
    public void shouldNotLoseMessagesOnExceptionInErrorHandler() throws Exception {
        template.sendBody(testingEndpoint, "Hello World");

        // as we handle new exception, then the exception is ignored
        // and causes the transaction to commit, so there is no message in the ActiveMQ DLQ queue
        Object dlqBody = consumer.receiveBody("activemq:ActiveMQ.DLQ", 2000);
        assertNull("Should not rollback the transaction", dlqBody);
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        // no redeliveries
        ConnectionFactory connectionFactory = HelperTest.createConnectionFactory(null, 0);
        JmsComponent component = jmsComponentTransacted(connectionFactory);
        camelContext.addComponent("activemq", component);
        return camelContext;
    }

}
