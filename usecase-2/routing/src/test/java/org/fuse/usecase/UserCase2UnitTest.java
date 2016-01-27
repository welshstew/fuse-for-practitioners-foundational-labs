package org.fuse.usecase;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by swinchester on 27/01/2016.
 */
@RunWith(CamelSpringJUnit4ClassRunner.class)
@UseAdviceWith
@ContextConfiguration(locations = "classpath*:META-INF/spring/camel-context.xml")
public class UserCase2UnitTest {

    @Autowired
    protected ModelCamelContext camelContext;

    public boolean useAdviceWith(){
        return true;
    }


    @Test
    public void test1() throws Exception {

        camelContext.getRouteDefinition("dbInsert").adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("sql:*")
                    .skipSendToOriginalEndpoint()
                    .to("mock:dbEndpoint");
            }
        });

        MockEndpoint me = (MockEndpoint) camelContext.getEndpoint("mock:dbEndpoint");
        me.setExpectedCount(1);

        camelContext.start();
        //wait for context to start
        Thread.sleep(5000);

        ProducerTemplate pt = camelContext.createProducerTemplate();
        pt.sendBody("direct:testFileRoute", "{\"company\":{\"active\":true,\"geo\":\"EU\",\"name\":\"CloudyCloud\"},\"contact\":{\"phone\":\"313-555-1234\",\"firstName\":\"Fred\",\"lastName\":\"Quicksand\",\"streetAddr\":\"202 Barney Blvd.\",\"state\":\"MI\",\"zip\":\"19728\",\"city\":\"Rock City\"}}");

        //wait for route
        Thread.sleep(5000);

        //should assert the map is equal

        me.assertIsSatisfied();
    }

}
