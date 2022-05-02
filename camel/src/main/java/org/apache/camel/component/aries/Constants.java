package org.apache.camel.component.aries;

public class Constants {

    // The ACA-Py API path 
    public static final String HEADER_SERVICE = "service";

    // The name of the TRUSTEE wallet that can onboard others to the ledger 
    public static final String HEADER_MULTITENANCY_TRUSTEE_WALLET = "multitenancy/trustee-wallet";
    
    // The Indy ledger role for this wallet
    public static final String HEADER_MULTITENANCY_LEDGER_ROLE = "multitenancy/ledger-role";
    
    // Allow this wallet to self register with ledger. This is cheating and required access to the ledger's admin interface
    public static final String HEADER_MULTITENANCY_SELF_REGISTER_NYM = "multitenancy/self-register-nym";
}