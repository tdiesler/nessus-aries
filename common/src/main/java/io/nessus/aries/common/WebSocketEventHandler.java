package io.nessus.aries.common;

import java.io.IOException;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.settings.Settings;
import org.hyperledger.aries.webhook.TenantAwareEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketEventHandler extends TenantAwareEventHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected WalletRecord myWallet;
    protected String myWalletName;
    protected String myWalletId;
    protected String serviceEndpoint;
    protected DID myPublicDid;

    protected AriesClient createClient() throws IOException {
        return Configuration.createClient(myWallet);
    }
    
    @Override
    public void handleSettings(String walletId, Settings settings) throws IOException {
        if (myWallet != null) {
            this.myWalletId = myWallet.getWalletId();
            this.myWalletName = myWallet.getSettings().getWalletName();
            this.myPublicDid = createClient().walletDidPublic().orElse(null);
        }
        this.serviceEndpoint = settings.getEndpoint();
        log.info("{} Settings: [{}] {}", myWalletName, walletId, settings);
        log.info("{} Public DID: {}", myWalletName, myPublicDid);
    }

    @Override
    public void handleConnection(String walletId, ConnectionRecord con) throws Exception {
        ConnectionState state = con.getState();
        String theirWalletName = WalletRegistry.getWalletName(walletId);
        log.info("{} Connection: [@{}] [{}] {}", myWalletName, theirWalletName, state, con);
    }
}