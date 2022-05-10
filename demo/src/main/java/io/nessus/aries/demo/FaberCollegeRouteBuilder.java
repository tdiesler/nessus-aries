package io.nessus.aries.demo;

import static io.nessus.aries.AgentConfiguration.assertSystemEnv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.google.sheets.GoogleSheetsComponent;
import org.apache.camel.component.google.sheets.GoogleSheetsConfiguration;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange.CredentialOfferDict;

import com.google.api.services.sheets.v4.model.ValueRange;

import io.nessus.aries.util.AssertState;

public class FaberCollegeRouteBuilder extends RouteBuilder {
    
    public final CountDownLatch messageLatch;
    
    private final List<String> processedRows = new ArrayList<>();
    private final GoogleSheetsConfiguration googleSheetsConfig;
    private final String spreadsheetId;
    
    public FaberCollegeRouteBuilder(CamelContext context, String spreadsheetId, CountDownLatch messageLatch) {
        this.spreadsheetId = spreadsheetId;
        this.messageLatch = messageLatch;
        
        String clientId = assertSystemEnv("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = assertSystemEnv("GOOGLE_OAUTH_CLIENT_SECRET");
        String refreshToken = assertSystemEnv("GOOGLE_OAUTH_REFRESH_TOKEN");
        
        googleSheetsConfig = new GoogleSheetsConfiguration(); 
        googleSheetsConfig.setApplicationName("Faber Transcript Demo");
        googleSheetsConfig.setClientId(clientId);
        googleSheetsConfig.setClientSecret(clientSecret);
        googleSheetsConfig.setRefreshToken(refreshToken);
    }

    @Override
    public void configure() {
        
        getCamelContext()
            .getComponent("google-sheets", GoogleSheetsComponent.class)
            .setConfiguration(googleSheetsConfig);
        
        // Repeatedly read the transscripts input data
        from("google-sheets://data/get?spreadsheetId=" + spreadsheetId + "&range=a1:h&delay=2000")
            .setBody(ex -> ex.getIn().getBody(ValueRange.class).getValues())
            .setProperty("SchemaAttributes", simple("${body[0]}"))
            .split(body())
                .choice()
                    .when(isHeaderRow)
                        .to("direct:processHeaderRow")
                        
                    .when(isActiveDataRow)
                        .to("direct:processDataRow")
                .end()
            .end();

        // Process the header row and create the Credential Definition
        from("direct:processHeaderRow")
            .setHeader("SchemaName", constant("Transscript"))
            .setHeader("SchemaVersion", constant("1.2"))
            .setHeader("SchemaAttributes", simple("${body}"))

            // Fetch already existing Credential Definitions
            .to("hyperledger-aries:faber?service=/credential-definitions/created")
            
            .choice()
                .when(simple("${body.credentialDefinitionIds.size} == 0"))
                    .to("direct:create-credential-definition")
                .otherwise()
                    .process(setGlobalOption("CredentialDefinitionId", simple("${body.credentialDefinitionIds[0]}")))
            .end();
        
        // Create a Credential Definition from header values
        from("direct:create-credential-definition")
            .log("Create Credential Definition ...")
            .setBody(ex -> Map.of(
                    "schemaName", simple.apply(ex, "${header.schemaName}"),
                    "schemaVersion", simple.apply(ex, "${header.schemaVersion}"),
                    "attributes", simple.apply(ex, "${header.schemaAttributes}"),
                    "supportRevocation", "false"
                ))
            .to("hyperledger-aries:faber?service=/credential-definitions&autoSchema=true")
            .process(setGlobalOption("CredentialDefinitionId", simple("${body.credentialDefinitionId}")));
        
        // Process a data row and create the Transscript Credential Offer 
        from("direct:processDataRow")
            .setBody(ex -> Map.of(
                    "cred_def_id", ex.getContext().getGlobalOption("CredentialDefinitionId"), 
                    "credential_preview", Map.of("attributes", credentialAttributes.apply(ex))))
            .to("hyperledger-aries:faber?service=/issue-credential/create-offer")
            .process(ex -> {
                List<String> keys = schemaAttributes.apply(ex);
                V1CredentialExchange credex = ex.getIn().getBody(V1CredentialExchange.class);
                CredentialOfferDict credDict = credex.getCredentialOfferDict();
                log.info(String.format("*******************************************************"));
                log.info(String.format("*"));
                log.info(String.format("* SchemaId:    %s", credex.getSchemaId()));
                log.info(String.format("* CredDefId:   %s", credex.getCredentialDefinitionId()));
                log.info(String.format("* Type:        %s", credDict.getType()));
                log.info(String.format("*"));
                credDict.getCredentialPreview().getAttributes().stream()
                    .sorted((a1, a2) -> keys.indexOf(a1.getName()) - keys.indexOf(a2.getName()))
                    .peek(at -> { if(at.getName().equals("ssn")) processedRows.add(at.getValue()); })
                    .forEach(at -> log.info(String.format("* %-12s %s", at.getName() + ":", at.getValue())));
                log.info(String.format("*"));
                log.info(String.format("*******************************************************"));
                log.info("");
            })
            .process(ex -> messageLatch.countDown());         
    }
    
    // Utility functions ------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    BiFunction<Exchange, Integer, String> token = (ex, idx) -> ((List<String>) ex.getIn().getBody(List.class)).get(idx); 
    BiFunction<Exchange, String, Object> simple = (ex, exp) -> new ValueBuilder(simple(exp)).evaluate(ex, Object.class); 
    
    Predicate isHeaderRow = ex -> token.apply(ex, 0).equals("first_name");
    Predicate isActiveDataRow = ex -> !token.apply(ex, 0).equals("first_name") && !processedRows.contains(token.apply(ex, 2)) && Integer.valueOf(token.apply(ex, 7)) == 1; 
    
    Function<Exchange, List<String>> schemaAttributes = ex -> Arrays.asList(ex.getProperty("SchemaAttributes", String[].class));
    
    Processor setGlobalOption(String key, Expression expr) { 
        return ex -> ex.getContext().getGlobalOptions().put(key, expr.evaluate(ex, String.class)); 
    }
    
    @SuppressWarnings("unchecked")
    Function<Exchange, List<CredentialAttributes>> credentialAttributes = ex -> { 
        List<String> keys = schemaAttributes.apply(ex);
        List<String> values = ex.getIn().getBody(List.class);
        AssertState.isEqual(keys.size(), values.size(), "Non matching keys/value");
        Map<String, Object> attrMap = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            attrMap.put(keys.get(i), values.get(i));
        }
        return CredentialAttributes.from(attrMap);
    };
}