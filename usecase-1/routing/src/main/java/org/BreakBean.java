package org;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Created by swinchester on 26/01/2016.
 */
public class BreakBean implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {

        String hello = "asdsad";
        
        String thing = hello + " ";
    }
}
