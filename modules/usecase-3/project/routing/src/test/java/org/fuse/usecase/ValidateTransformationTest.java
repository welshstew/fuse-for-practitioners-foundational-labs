package org.fuse.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;

public class ValidateTransformationTest extends CamelSpringTestSupport {

    @EndpointInject(uri = "mock:csv2json-test-output") private MockEndpoint resultEndpoint;

    @Produce(uri = "direct:csv2json-test-input") private ProducerTemplate startEndpoint;

    @Test public void transform() throws Exception {
        // setup expectations
        resultEndpoint.expectedMessageCount(1);
        // set expected body as the unpretty print version of the json
        // (flattened)
        resultEndpoint.expectedBodiesReceived(jsonUnprettyPrint(readFile("src/test/data/account.json")));

        // run test
        startEndpoint.sendBody(readFile("src/test/data/customer.csv"));

        // verify results
        resultEndpoint.assertIsSatisfied();
    }

    @Override protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:csv2json-test-input")
                   .log("Before transformation:\n ${body}")
                   .to("ref:csv2json")
                   .log("After transformation:\n ${body}")
                   .to("mock:csv2json-test-output");
            }
        };
    }

    @Override protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml");
    }

    private String readFile(String filePath) throws Exception {
        String content;
        FileInputStream fis = new FileInputStream(filePath);
        try {
            content = createCamelContext().getTypeConverter().convertTo(String.class, fis);
        } finally {
            fis.close();
        }
        return content;
    }

    private String jsonUnprettyPrint(String jsonString) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        JsonNode node = mapper.readTree(jsonString);
        return node.toString();
    }
}
