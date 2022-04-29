/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.xchange.metadata;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aries.HyperledgerAriesComponent;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.hyperledger.acy_py.generated.model.DID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

//@EnabledIfSystemProperty(named = "enable.aries.itests", matches = "true", disabledReason = "Requires API credentials")
public class WalletServiceProducerTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:publicDid")
                    .to("hyperledger-aries:government?service=wallet&walletCreate=true&walletRemove=true&walletRole=endorser&method=publicDid");
            }
        };
    }

    @Test
    void testPublicDID() {

        HyperledgerAriesComponent component = context.getComponent("hyperledger-aries", HyperledgerAriesComponent.class);
        Assertions.assertNotNull(component, "Component not null");
        
        DID publicDid = template.requestBody("direct:publicDid", "gov", DID.class);
        Assertions.assertNotNull(publicDid, "publicDid not null");
    }
}
