package io.nessus.aries.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.aries.api.multitenancy.WalletRecord;

public class WalletRegistry {
    
    private static final Map<String, WalletRecord> walletsCache = Collections.synchronizedMap(new HashMap<>());

    public static void putWallet(WalletRecord wallet) {
        walletsCache.put(wallet.getWalletId(), wallet);
    }
    
    public static void removeWallet(String walletId) {
        walletsCache.remove(walletId);
    }

    public static WalletRecord getWallet(String walletId) {
        return walletsCache.get(walletId);
    }

    public static String getWalletName(String walletId) {
        WalletRecord wallet = WalletRegistry.getWallet(walletId);
        return wallet != null ? wallet.getSettings().getWalletName() : null;
    }
}