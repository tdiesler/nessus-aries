package org.apache.camel.component.aries.handler;

import org.apache.camel.Exchange;

public interface ServiceHandler {

    void process(Exchange exchange) throws Exception;

}