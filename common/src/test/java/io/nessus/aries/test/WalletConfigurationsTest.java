
package io.nessus.aries.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.wallet.WalletConfigurations;
import io.nessus.aries.wallet.WalletConfigurations.WalletConfig;

public class WalletConfigurationsTest extends AbstractTest {

    @Test
    void tesUnauthorized() throws Exception {

        WalletConfigurations configs = WalletConfigurations.loadWalletConfigurations();
        
        WalletConfig faber = configs.getWalletConfig("Faber");
        AgentConfiguration def = AgentConfiguration.defaultConfiguration();
        Assertions.assertEquals(def.getAdminUrl(), faber.endpoint);
        Assertions.assertEquals(def.getApiKey(), faber.api_key);
        Assertions.assertNotNull(faber.auth_token);
    }
}
