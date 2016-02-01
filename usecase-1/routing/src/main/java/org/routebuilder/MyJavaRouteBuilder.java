package org.routebuilder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public class MyJavaRouteBuilder extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		// TODO Auto-generated method stub
		from("timer://hello3?period=5000").routeId(createRouteId("hello3"))
		.log(LoggingLevel.DEBUG, "hello3 world");
	}


	private String createRouteId(String routeName){
		return this.getClass().getCanonicalName() + "." + routeName;
	}

}
