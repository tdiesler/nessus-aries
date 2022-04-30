package org.apache.camel.component.aries;

public class UnsupportedServiceException extends RuntimeException {
    
    private static final long serialVersionUID = 8827347527702393577L;

    public UnsupportedServiceException(String service) {
        super("Unsupported service: " + service);
    }
}