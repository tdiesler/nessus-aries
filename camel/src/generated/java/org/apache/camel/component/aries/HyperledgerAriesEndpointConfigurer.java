/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.aries;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExtendedPropertyConfigurerGetter;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.spi.ConfigurerStrategy;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.support.component.PropertyConfigurerSupport;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class HyperledgerAriesEndpointConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        HyperledgerAriesEndpoint target = (HyperledgerAriesEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "lazystartproducer":
        case "lazyStartProducer": target.setLazyStartProducer(property(camelContext, boolean.class, value)); return true;
        case "schemaname":
        case "schemaName": target.getConfiguration().setSchemaName(property(camelContext, java.lang.String.class, value)); return true;
        case "schemaversion":
        case "schemaVersion": target.getConfiguration().setSchemaVersion(property(camelContext, java.lang.String.class, value)); return true;
        case "service": target.getConfiguration().setService(property(camelContext, java.lang.String.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Class<?> getOptionType(String name, boolean ignoreCase) {
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "lazystartproducer":
        case "lazyStartProducer": return boolean.class;
        case "schemaname":
        case "schemaName": return java.lang.String.class;
        case "schemaversion":
        case "schemaVersion": return java.lang.String.class;
        case "service": return java.lang.String.class;
        default: return null;
        }
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        HyperledgerAriesEndpoint target = (HyperledgerAriesEndpoint) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "lazystartproducer":
        case "lazyStartProducer": return target.isLazyStartProducer();
        case "schemaname":
        case "schemaName": return target.getConfiguration().getSchemaName();
        case "schemaversion":
        case "schemaVersion": return target.getConfiguration().getSchemaVersion();
        case "service": return target.getConfiguration().getService();
        default: return null;
        }
    }
}

