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

import java.util.Optional;

import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange.CredentialProposalDict.CredentialProposal;

// [TODO] Pull up to acapy-java-client
public class CredentialProposalHelper {
    
    private final CredentialProposal credentialProposal;
    
    public CredentialProposalHelper(CredentialProposal credentialProposal) {
        this.credentialProposal = credentialProposal;
    }
    
    public Optional<CredentialAttributes> getAttribute(String name) {
        return credentialProposal.getAttributes().stream().filter(at -> at.getName().equals(name)).findFirst();
    }
    
    public String getAttributeValue(String name) {
        CredentialAttributes attr = getAttribute(name).orElse(null);
        return attr != null ? attr.getValue() : null;
    }
}
