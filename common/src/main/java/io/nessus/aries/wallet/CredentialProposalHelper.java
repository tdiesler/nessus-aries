package io.nessus.aries.wallet;

import java.util.Optional;

import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange.CredentialProposalDict.CredentialProposal;

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