package org.fuse.usecase.routebuilder;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;

/**
 * Created by swinchester on 26/01/2016.
 */
public class UseCase2RouteBuilder extends RouteBuilder {

    @EndpointInject(uri = "{{fileInput}}")
    Endpoint jsonFileInput;

    @EndpointInject(uri = "{{fileOutput}}")
    Endpoint fileOutput;




    @Override
    public void configure() throws Exception {

    }
}
