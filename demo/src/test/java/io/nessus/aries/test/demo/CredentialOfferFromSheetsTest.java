package io.nessus.aries.test.demo;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.nessus.aries.demo.FaberCollegeRouteBuilder;

@EnabledIfEnvironmentVariable(named = "GOOGLE_OAUTH_CLIENT_ID", matches = ".+")
public class CredentialOfferFromSheetsTest extends AbstractCamelAriesTest {

    CountDownLatch messageLatch = new CountDownLatch(3);

    @Override
    protected RouteBuilder createRouteBuilder() {
        String spreadsheetId = "1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw";
        return new FaberCollegeRouteBuilder(context, spreadsheetId, messageLatch);
    }
    
    @Test
    public void testWorkflow() throws Exception {

        setRemoveWalletsOnShutdown(true);
        
        onboardWallet(Faber, ENDORSER);
        
        Assertions.assertTrue(messageLatch.await(300, TimeUnit.SECONDS));
    }
}
