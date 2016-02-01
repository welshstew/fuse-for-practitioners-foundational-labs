package org.fuse.usecase.routebuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.acme.Customer;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.fuse.usecase.processor.SqlParameterProcessorBean;
import org.globex.Account;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by swinchester on 28/01/2016.
 */
public class UseCase3RouteBuilder extends RouteBuilder {

    @EndpointInject(uri="{{fileInput}}")
    Endpoint inputEndpoint;

    @EndpointInject(uri="direct:doTransform")
    Endpoint transformEndpoint;

    @EndpointInject(uri="{{errorTopic}}")
    Endpoint errorEndpoint;

    @EndpointInject(ref="direct-error-queue")
    Endpoint exceptionProcessing;

    @EndpointInject(uri = "{{sqlInsertSuccess}}")
    Endpoint sqlInsertSuccessEndpoint;

    @EndpointInject(uri = "{{sqlInsertError}}")
    Endpoint sqlInsertErrorEndpoint;

    @EndpointInject(uri="direct:insertDB")
    Endpoint insertIntoDBEndpoint;

    @EndpointInject(uri="{{sqlPollFixed}}")
    Endpoint sqlPollFixedEndpoint;

    SqlParameterProcessorBean sqlParameterBean = new SqlParameterProcessorBean();


    @Override
    public void configure() throws Exception {

        DataFormat dfBindy = new BindyCsvDataFormat(Customer.class);

        JacksonDataFormat jdf = new JacksonDataFormat(Account.class);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jdf.disableFeature(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        onException(IllegalArgumentException.class)
                .handled(true)
                .log(LoggingLevel.INFO, "Sending message to error topic: ${body}")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hello = "";
                    }
                })
                .setHeader(Exchange.EXCEPTION_CAUGHT, simple("${exception} ${exception.message}"))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hello = "";
                    }
                })
                .to(errorEndpoint);


        from(inputEndpoint)
                .split().tokenize("\n")
                    .to(transformEndpoint)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hello ="";
                    }
                })
                    .to(insertIntoDBEndpoint);

        from(transformEndpoint)
                .unmarshal(dfBindy)
                .transform().groovy("resource:classpath:transform.groovy")
                .unmarshal(jdf)
                .log("${body}");

        from(exceptionProcessing).routeId("org.fuse.usecase").log(LoggingLevel.DEBUG, "${body}")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    //here we need to add headers for the sqlerror endpoint
                    exchange.getIn().setHeader("errorcode", 1);
                    exchange.getIn().setHeader("errortext", exchange.getIn().getHeader(Exchange.EXCEPTION_CAUGHT));
                    exchange.getIn().setHeader("errormessage", exchange.getIn().getBody());
                    exchange.getIn().setHeader("errorstatus", "ERROR");
                }
            }).to(sqlInsertErrorEndpoint);

        from(insertIntoDBEndpoint).routeId("dbInsert")
                .bean(sqlParameterBean, "defineNamedParameters")
                .to(sqlInsertSuccessEndpoint)
                .log("Inserted into DB!");

        from(errorEndpoint)
                .to(exceptionProcessing);

        //resubmit the message once it has been fixed in the DB
        from(sqlPollFixedEndpoint)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hi = "";
                    }
                })
                .setHeader("ID", simple("${body['ID']}"))
                .setBody(simple("${body['MESSAGE']}"))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String hi = "";
                    }
                })
                .to(transformEndpoint)
                .to(insertIntoDBEndpoint);

    }
}
