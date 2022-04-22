package io.nessus.aries.common;

import java.io.IOException;
import java.util.Optional;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.ConnectionTheirRole;
import org.hyperledger.aries.api.multitenancy.WalletRecord;
import org.hyperledger.aries.api.settings.Settings;
import org.hyperledger.aries.webhook.TenantAwareEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketEventHandler extends TenantAwareEventHandler {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private WalletRegistry walletRegistry;
    private WalletRecord thisWallet;
    private String serviceEndpoint;

    public WebSocketEventHandler walletRegistry(WalletRegistry walletRegistry) {
        this.walletRegistry = walletRegistry;
        return this;
    }
    
    void setThisWallet(WalletRecord thisWallet) {
        this.thisWallet = thisWallet;
    }

    protected Optional<WalletRecord> getTheirWallet(String walletId) {
        Optional<WalletRecord> result = Optional.ofNullable(null);
        if (walletRegistry != null) 
            result = walletRegistry.getWallet(walletId);
        return result;
    }
    
    protected String getTheirWalletName(String walletId) {
        Optional<WalletRecord> optional = getTheirWallet(walletId);
        return optional.isPresent() ? optional.get().getSettings().getWalletName() : null;
    }
    
    protected AriesClient createClient() throws IOException {
        return Configuration.createClient(thisWallet);
    }
    
    @Override
    public void handleSettings(String walletId, Settings settings) throws IOException {
        this.serviceEndpoint = settings.getEndpoint();
        log.info("{} Settings: [{}] {}", thisWalletName(), walletId, settings);
    }

    @Override
    public void handleConnection(String walletId, ConnectionRecord con) throws Exception {
        ConnectionState state = con.getState();
        ConnectionTheirRole theirRole = con.getTheirRole();
        String theirWalletName = getTheirWalletName(walletId);
        log.info("{} Connection: [@{}] {} {} {}", thisWalletName(), theirWalletName, theirRole, state, con);
    }
    
    protected WalletRecord getThisWallet() {
        return thisWallet;
    }

    protected String thisWalletId() {
        return thisWallet.getWalletId();
    }
    
    protected String thisWalletName() {
        return thisWallet.getSettings().getWalletName();
    }

    protected String getServiceEndpoint() {
        return serviceEndpoint;
    }
}