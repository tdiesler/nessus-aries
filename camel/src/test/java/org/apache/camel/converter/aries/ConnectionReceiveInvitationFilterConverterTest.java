package org.apache.camel.converter.aries;

import java.util.Map;

import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.config.GsonConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class ConnectionReceiveInvitationFilterConverterTest {

    static final Gson gson = GsonConfig.defaultConfig();
    
    @Test
    public void testTypeConcertion() throws Exception {

        ConnectionReceiveInvitationFilter obj = ConnectionReceiveInvitationFilterConverter.toFilter(Map.of("auto_accept", true));
        Assertions.assertTrue(obj.getAutoAccept());

        obj = ConnectionReceiveInvitationFilterConverter.toFilter(Map.of("auto_accept", "true", "foo", "bar"));
        Assertions.assertTrue(obj.getAutoAccept());

        obj = ConnectionReceiveInvitationFilterConverter.toFilter(Map.of("foo", "bar"));
        Assertions.assertNull(obj);

        obj = ConnectionReceiveInvitationFilterConverter.toFilter("{}");
        Assertions.assertNull(obj);

        obj = ConnectionReceiveInvitationFilterConverter.toFilter("invalid");
        Assertions.assertNull(obj);
    }
    
    @Test
    public void testSnakeCase() throws Exception {
        
        String res = ConnectionReceiveInvitationFilterConverter.toSnakeCase("credentialDefinitionId");
        Assertions.assertEquals("credential_definition_id", res);
    }
}
