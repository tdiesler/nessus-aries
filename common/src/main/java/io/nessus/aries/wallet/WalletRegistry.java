package io.nessus.aries.wallet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hyperledger.aries.api.multitenancy.WalletRecord;

public class WalletRegistry {
    
    private final Map<String, WalletRecord> walletsCache = Collections.synchronizedMap(new HashMap<>());

    public WalletRegistry() {
    }

    public WalletRegistry(WalletRecord... wallets) {
        Arrays.asList(wallets).forEach(w -> putWallet(w));
    }

    public void putWallet(WalletRecord wallet) {
        walletsCache.put(wallet.getWalletId(), wallet);
    }
    
    public void removeWallet(String walletId) {
        walletsCache.remove(walletId);
    }

    public Optional<WalletRecord> getWallet(String walletId) {
        return Optional.ofNullable(walletsCache.get(walletId));
    }

    public String getWalletName(String walletId) {
        WalletRecord wallet = walletsCache.get(walletId);
        return wallet != null ? wallet.getSettings().getWalletName() : null;
    }
}