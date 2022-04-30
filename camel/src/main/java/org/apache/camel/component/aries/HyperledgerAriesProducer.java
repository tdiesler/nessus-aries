/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aries;

import static org.apache.camel.component.aries.Constants.HEADER_SERVICE;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.handler.MultitenancyServiceHandler;
import org.apache.camel.component.aries.handler.ServiceHandler;
import org.apache.camel.component.aries.handler.WalletServiceHandler;
import org.apache.camel.support.DefaultProducer;

import io.nessus.aries.util.AssertState;

public class HyperledgerAriesProducer extends DefaultProducer {

    public HyperledgerAriesProducer(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public HyperledgerAriesEndpoint getEndpoint() {
        return (HyperledgerAriesEndpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        ServiceHandler serviceHandler;
        
        String service = getService(exchange);
        if (service.startsWith("/multitenancy")) {
            serviceHandler = new MultitenancyServiceHandler(getEndpoint(), service);
        }
        else if (service.startsWith("/wallet")) {
            serviceHandler = new WalletServiceHandler(getEndpoint(), service);
        }
        else throw new UnsupportedServiceException(service);
        
        serviceHandler.process(exchange);
    }

    private String getService(Exchange exchange) {
        String service = getEndpoint().getConfiguration().getService();
        if (service == null)
            service = exchange.getIn().getHeader(HEADER_SERVICE, String.class);
        AssertState.notNull(service, "Cannot obtain API service");
        if (!service.startsWith("/")) 
            service = "/" + service;
        return service;
    }
}
