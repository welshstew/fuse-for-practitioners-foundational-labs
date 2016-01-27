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
import org.fuse.usecase.service.CustomerWSImpl;
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

//    @EndpointInject(ref="customerWebService")
//    Endpoint customerWebService;

    @EndpointInject(uri = "direct:callWSEndpoint")
    Endpoint directCallWS;

    @EndpointInject(uri = "direct:callRestEndpoint")
    Endpoint directCallREST;

    @EndpointInject(uri="cxfrs:bean:customerRestService")
    Endpoint customerRESTServiceEndpoint;

    @EndpointInject(uri="cxf:bean:customerServiceEndpoint")
    Endpoint customerSOAPServiceEndpoint;

    CustomerWSImpl wsImpl = new CustomerWSImpl();


    @Override
    public void configure() throws Exception {

        JacksonDataFormat jdf = new JacksonDataFormat(Account.class);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        from(customerRESTServiceEndpoint).setExchangePattern(ExchangePattern.InOut)
                .bean(CustomerRestImpl.class, "enrich");

        from(customerSOAPServiceEndpoint)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String  hello = "";
                    }
                }).bean(wsImpl, "updateAccount");

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
                .setHeader(Exchange.ACCEPT_CONTENT_TYPE, constant("application/json"))
                .setHeader(CxfConstants.OPERATION_NAME, constant("enrich"))
                .setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, constant(false))
                .to("cxfrs:bean:customerRestServiceClient");

        from(directCallWS)
                .setHeader(CxfConstants.OPERATION_NAME, constant("updateAccount"))
                .setHeader(CxfConstants.OPERATION_NAMESPACE, constant("http://service.usecase.fuse.org/"))
                .log("hello");
//                .to("cxf:customerWebService?serviceClass=org.fuse.usecase.service.CustomerWS");


    }

}
