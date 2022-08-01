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

package io.nessus.aries.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.nessus.aries.AgentConfiguration;
import io.nessus.aries.wallet.WalletConfigurations;
import io.nessus.aries.wallet.WalletConfigurations.WalletConfig;

public class WalletConfigurationsTest extends AbstractTest {

    @Test
    void tesUnauthorized() throws Exception {

        String home = System.getProperty("user.home");
        File infile = new File(home + "/.config/aries-cli/config.yaml");
        assumeTrue(infile.isFile());
        
        WalletConfigurations configs = WalletConfigurations.loadWalletConfigurations();
        
        WalletConfig faber = configs.getWalletConfig("Faber");
        AgentConfiguration def = AgentConfiguration.defaultConfiguration();
        Assertions.assertEquals(def.getAdminUrl(), faber.endpoint);
        Assertions.assertEquals(def.getApiKey(), faber.api_key);
        Assertions.assertNotNull(faber.auth_token);
    }
}
