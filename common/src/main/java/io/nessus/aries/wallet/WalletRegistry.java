/*-
 * #%L
 * Nessus Aries :: Common
 * %%
 * Copyright (C) 2022 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.nessus.aries.wallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WalletRegistry {
    
    private final Map<String, NessusWallet> walletsCache = Collections.synchronizedMap(new LinkedHashMap<>());

    public WalletRegistry(NessusWallet... wallets) {
        Arrays.asList(wallets).forEach(w -> putWallet(w));
    }

    public List<String> getWalletNames() {
        return walletsCache.values().stream()
                .map(w -> w.getSettings().getWalletName())
                .collect(Collectors.toList());
    }
    
    public void putWallet(NessusWallet wallet) {
        walletsCache.put(wallet.getWalletId(), wallet);
    }
    
    public void removeWallet(String walletId) {
        walletsCache.remove(walletId);
    }

    public List<NessusWallet> getWallets() {
        return new ArrayList<>(walletsCache.values());
    }
    
    public NessusWallet getWallet(String walletId) {
        return walletsCache.get(walletId);
    }

    public String getWalletName(String walletId) {
        NessusWallet wallet = walletsCache.get(walletId);
        return wallet != null ? wallet.getSettings().getWalletName() : null;
    }

    public NessusWallet getWalletByName(String walletName) {
        return walletsCache.values().stream()
                .filter(w -> w.getSettings().getWalletName().equalsIgnoreCase(walletName))
                .findAny().orElse(null);
    }
}
