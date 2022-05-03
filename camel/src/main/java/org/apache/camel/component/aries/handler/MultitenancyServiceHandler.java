package org.apache.camel.component.aries.handler;

import static io.nessus.aries.wallet.WalletBuilder.selfRegisterWithDid;
import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_LEDGER_ROLE;
import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_SELF_REGISTER_NYM;
import static org.apache.camel.component.aries.Constants.HEADER_MULTITENANCY_TRUSTEE_WALLET;

import org.apache.camel.Exchange;
import org.apache.camel.component.aries.HyperledgerAriesEndpoint;
import org.apache.camel.component.aries.UnsupportedServiceException;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.acy_py.generated.model.DIDEndpoint;
import org.hyperledger.acy_py.generated.model.RegisterLedgerNymResponse;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.api.ledger.RegisterNymFilter;
import org.hyperledger.aries.api.multitenancy.CreateWalletRequest;

import io.nessus.aries.util.AssertState;
import io.nessus.aries.wallet.NessusWallet;

public class MultitenancyServiceHandler extends AbstractServiceHandler {
    
    public MultitenancyServiceHandler(HyperledgerAriesEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange, String service) throws Exception {
        if (service.equals("/multitenancy/wallet")) {
            CreateWalletRequest walletRequest = assertBody(exchange, CreateWalletRequest.class);
            String walletName = walletRequest.getWalletName();
            
            boolean selfRegister = getHeader(exchange, HEADER_MULTITENANCY_SELF_REGISTER_NYM, boolean.class);
            IndyLedgerRoles ledgerRole = getHeader(exchange, HEADER_MULTITENANCY_LEDGER_ROLE, IndyLedgerRoles.class);
            String trusteeName = getHeader(exchange, HEADER_MULTITENANCY_TRUSTEE_WALLET, String.class);
            
            NessusWallet walletRecord = NessusWallet.build(baseClient().multitenancyWalletCreate(walletRequest).get());
            
            String walletId = walletRecord.getWalletId();
            log.info("{}: [{}] {}", walletName, walletId, walletRecord);

            getComponent().addWallet(walletRecord);
            
            if (ledgerRole != null) {
                
                AssertState.isTrue(selfRegister || trusteeName != null, "LedgerRole " + ledgerRole + " requires selfRegister or trusteeWallet");
                
                // Create a local DID for the wallet
                AriesClient client = createClient(walletRecord);
                DID did = client.walletDidCreate(DIDCreate.builder().build()).get();
                log.info("{}: {}", walletName, did);
                
                if (trusteeName != null) {
                    
                    NessusWallet trusteeWallet = getComponent().getWallet(trusteeName);
                    AssertState.notNull(trusteeWallet, "Cannot obtain trustee wallet: " + trusteeName);
                    
                    AriesClient trustee = createClient(trusteeWallet);
                    RegisterLedgerNymResponse nymResponse = trustee.ledgerRegisterNym(RegisterNymFilter.builder()
                            .did(did.getDid())
                            .verkey(did.getVerkey())
                            .role(ledgerRole)
                            .build()).get();
                    log.info("{} for {}: {}", trusteeName, walletName, nymResponse);
                } 
                else if (selfRegister) {
                    // Register DID with the leder (out-of-band)
                    selfRegisterWithDid(walletName, did.getDid(), did.getVerkey(), ledgerRole);
                }
                
                // Set the public DID for the wallet
                client.walletDidPublic(did.getDid());
                
                DIDEndpoint didEndpoint = client.walletGetDidEndpoint(did.getDid()).get();
                log.info("{}: {}", walletName, didEndpoint);
            } 
            
            exchange.getIn().setBody(walletRecord);
        }
        else throw new UnsupportedServiceException(service);
    }
}