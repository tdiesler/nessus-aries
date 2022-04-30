package org.apache.camel.component.aries.handler;

import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_WALLET_REGISTER_NYM;
import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_WALLET_ROLE;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;
import org.hyperledger.aries.api.multitenancy.WalletRecord;

import io.nessus.aries.wallet.WalletBuilder;

public class MultitenancyServiceHandler extends AbstractServiceHandler {
    
    public MultitenancyServiceHandler(HyperledgerAriesEndpoint endpoint, String service) {
        super(endpoint, service);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (service.equals("/multitenancy/wallet")) {
            CreateWalletRequest walletRequest = assertBody(exchange, CreateWalletRequest.class);
            WalletRecord walletRecord = baseClient().multitenancyWalletCreate(walletRequest).get();
            String walletName = walletRecord.getSettings().getWalletName();
            getComponent().addWallet(walletRecord);
            exchange.getIn().setBody(walletRecord);
            if (getHeader(exchange, HEADER_MULTITENANCY_WALLET_REGISTER_NYM, boolean.class)) {
                // Create a local DID for the wallet
                AriesClient client = createClient(walletRecord);
                DID did = client.walletDidCreate(DIDCreate.builder().build()).get();
                log.info("{}: {}", walletName, did);
                // Self register the public DID - this is cheating
                IndyLedgerRoles role = getHeader(exchange, HEADER_MULTITENANCY_WALLET_ROLE, IndyLedgerRoles.class);
                WalletBuilder.selfRegisterWithDid(walletName, did.getDid(), did.getVerkey(), role);
                client.walletDidPublic(did.getDid());
            }
        }
        else throw new UnsupportedServiceException(service);
    }
}