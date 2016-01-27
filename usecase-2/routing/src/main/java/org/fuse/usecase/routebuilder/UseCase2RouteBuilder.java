package org.fuse.usecase.routebuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.DataFormat;
import org.fuse.usecase.aggregate.UseCase2AggregationStrategy;
import org.fuse.usecase.service.CustomerRestImpl;
import org.globex.Account;

/**
 * Created by swinchester on 26/01/2016.
 */
public class UseCase2RouteBuilder extends RouteBuilder {

    @EndpointInject(uri = "{{fileInput}}")
    Endpoint jsonFileInput;

    @EndpointInject(uri = "{{fileOutput}}")
    Endpoint fileOutput;

    @EndpointInject(uri = "{{fileError}}")
    Endpoint fileError;

    @EndpointInject(ref="customerWebService")
    Endpoint customerWebService;

    @EndpointInject(uri = "direct:callWSEndpoint")
    Endpoint directCallWS;

    @EndpointInject(uri = "direct:callRestEndpoint")
    Endpoint directCallREST;

    @EndpointInject(uri="cxfrs:bean:customerRestService")
    Endpoint customerRESTServiceEndpoint;




    @Override
    public void configure() throws Exception {

        JacksonDataFormat jdf = new JacksonDataFormat(Account.class);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        from(customerRESTServiceEndpoint).setExchangePattern(ExchangePattern.InOut)
                .bean(CustomerRestImpl.class, "enrich");

        from(jsonFileInput).routeId("mainJsonFileRoute")
                .convertBodyTo(String.class)
                .log("${body}")
                .unmarshal(jdf)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hello = "";
                    }
                })
                .multicast(new UseCase2AggregationStrategy()).to(directCallWS, directCallREST)
                .end();

        from(directCallREST)
                .setHeader("Content-Type", constant("application/json"))
                .setHeader(Exchange.ACCEPT_CONTENT_TYPE, constant("application/json"))
                .setHeader(CxfConstants.OPERATION_NAME, constant("enrich"))
                .setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, constant(false))
                .to("cxfrs:bean:customerRestServiceClient");

        from(directCallWS)
                .log("HELLO2");
//                .to("cxf:customerWebService?serviceClass=org.fuse.usecase.service.CustomerWS");


    }

}
