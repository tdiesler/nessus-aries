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
package org.apache.camel.converter.aries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Converter;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.config.GsonConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@Converter(generateLoader = true)
public final class ConnectionReceiveInvitationFilterConverter {

    static final Gson gson = GsonConfig.defaultConfig();
    
    @Converter
    public static ConnectionReceiveInvitationFilter toFilter(JsonObject jsonObj) {
        ConnectionReceiveInvitationFilter result = null;
        if (filterFields(ConnectionReceiveInvitationFilter.class, jsonObj)) 
            result = gson.fromJson(jsonObj, ConnectionReceiveInvitationFilter.class);
        return result;
    }
    
    @Converter
    public static ConnectionReceiveInvitationFilter toFilter(String json) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            return toFilter(obj);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
    
    @Converter
    public static ConnectionReceiveInvitationFilter toFilter(Map<String, Object> map) {
        String json = gson.toJson(map);
        return toFilter(json);
    }
    
    private static boolean filterFields(Class<?> type, JsonObject json) {
        
        // Convert target object field names to snake case
        List<String> acceptedFields = Arrays.asList(type.getDeclaredFields()).stream()
                .map(f -> toSnakeCase(f.getName()))
                .collect(Collectors.toList());

        // Filter unexpected properties
        new HashSet<>(json.keySet()).stream().forEach(k -> {
            if (!acceptedFields.contains(k))
                json.remove(k);
        });
        
        return !json.keySet().isEmpty();
    }
    
    public static String toSnakeCase(String instr) {
        return instr.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
