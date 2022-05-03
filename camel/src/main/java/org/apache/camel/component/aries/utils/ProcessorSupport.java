package org.apache.camel.component.aries.utils;

import static org.apache.camel.component.aries.Constants.HEADER_NESSUS_WALLET;
import static org.apache.camel.component.aries.Constants.HEADER_WALLET_NAME;
import static org.apache.camel.component.aries.Constants.PROPERTY_HYPERLEDGER_ARIES_COMPONENT;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesComponent;

import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;

public class ProcessorSupport {
    
    public static HyperledgerAriesComponent getHyperledgerAriesComponent(Exchange exchange) {
        HyperledgerAriesComponent component = exchange.getProperty(PROPERTY_HYPERLEDGER_ARIES_COMPONENT, HyperledgerAriesComponent.class);
        AssertState.notNull(component, "Cannot find exchange property: " + PROPERTY_HYPERLEDGER_ARIES_COMPONENT);
        return component;
    }
    
    public static String getWalletName(Exchange exchange) {
        String walletName = exchange.getIn().getHeader(HEADER_WALLET_NAME, String.class);
        AssertState.notNull(walletName, "Cannot find exchange property: " + HEADER_WALLET_NAME);
        return walletName;
    }
    
    public static NessusWallet getWallet(Exchange exchange) {
        NessusWallet walletRecord = exchange.getIn().getHeader(HEADER_NESSUS_WALLET, NessusWallet.class);
        AssertState.notNull(walletRecord, "Cannot find exchange property: " + HEADER_NESSUS_WALLET);
        return walletRecord;
    }
}