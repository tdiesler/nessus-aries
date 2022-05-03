package org.apache.camel.component.aries.utils;

import static org.apache.camel.component.aries.Constants.PROPERTY_HYPERLEDGER_ARIES_COMPONENT;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesComponent;

import io.nessus.aries.util.AssertState;

public class ProcessorSupport {
    
    public static HyperledgerAriesComponent getHyperledgerAriesComponent(Exchange exchange) {
        HyperledgerAriesComponent component = exchange.getProperty(PROPERTY_HYPERLEDGER_ARIES_COMPONENT, HyperledgerAriesComponent.class);
        AssertState.notNull(component, "HyperledgerAriesComponent not available from exchange");
        return component;
    }
}