package io.nessus.aries.demo;

import static org.hyperledger.aries.api.ledger.IndyLedgerRoles.ENDORSER;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.component.aries.HyperledgerAriesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.aries.wallet.NessusWallet;
import io.nessus.aries.wallet.WalletBuilder;
import io.nessus.aries.wallet.WalletRegistry;

/**

docker run -it --rm \
    -e GOOGLE_OAUTH_CLIENT_ID=${GOOGLE_OAUTH_CLIENT_ID} \
    -e GOOGLE_OAUTH_CLIENT_SECRET=${GOOGLE_OAUTH_CLIENT_SECRET} \
    -e GOOGLE_OAUTH_REFRESH_TOKEN=${GOOGLE_OAUTH_REFRESH_TOKEN} \
    -e INDY_WEBSERVER_HOSTNAME=host.docker.internal \
    -e ACAPY_HOSTNAME=host.docker.internal \
    nessusio/aries-demo \
        --spreadsheet-id=1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw
    
*/
public class Main {

    static final Logger log = LoggerFactory.getLogger(Main.class);
    
    static class DemoOptions {
        
        @Option(name = "--spreadsheet-id", metaVar = "1D2RogwD1...", required = true, usage = "Google Sheets Id")
        public String spreadsheetId;

        @Option(name = "--max-message-count", usage = "Number of messages before the app exits")
        public Integer maxMessageCount = 5;
    }
    
    public void run(String[] args) throws Exception {
        
        DemoOptions opts = new DemoOptions();
        CmdLineParser parser = new CmdLineParser(opts);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            parser.printUsage(System.err);
            return;
        }
        
        try (CamelContext camelctx = new DefaultCamelContext()) {
            
            CountDownLatch messageLatch = new CountDownLatch(opts.maxMessageCount);
            camelctx.addRoutes(new FaberCollegeRouteBuilder(camelctx, opts.spreadsheetId, messageLatch));
            
            // Remove the wallets created by this component on shutdown
            HyperledgerAriesComponent component = getHyperledgerAriesComponent(camelctx);
            component.setRemoveWalletsOnShutdown(true);
            
            onboardWallet(camelctx, "Faber", ENDORSER);
            
            camelctx.start();
            
            messageLatch.await();
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    private HyperledgerAriesComponent getHyperledgerAriesComponent(CamelContext camelctx) {
        return camelctx.getComponent("hyperledger-aries", HyperledgerAriesComponent.class);
    }

    private NessusWallet onboardWallet(CamelContext camelctx, String walletName, IndyLedgerRoles role) throws IOException {

        logSection("Onboard " + walletName);
        
        WalletRegistry walletRegistry = getHyperledgerAriesComponent(camelctx).getWalletRegistry();
        
        NessusWallet walletRecord = new WalletBuilder(walletName)
                .walletRegistry(walletRegistry)
                .selfRegisterNym(role != null)
                .ledgerRole(role)
                .build();
        
        return walletRecord;
    }

    private void logSection(String title) {
        int len = 119 - title.length();
        char[] tail = new char[len];
        Arrays.fill(tail, '=');
        log.info("{} {}", title, String.valueOf(tail));
    }
}