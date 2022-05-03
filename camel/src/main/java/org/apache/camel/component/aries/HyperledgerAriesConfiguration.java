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
package org.apache.camel.component.aries;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.spi.UriPath;

@UriParams
public class HyperledgerAriesConfiguration {

    @UriPath(description = "The wallet to connect to")
    @Metadata(required = true)
    private String wallet;
    @UriParam(description = "The path to call")
    @Metadata(required = false)
    private String service;
    @UriParam(description = "A schema name when omitted from the payload")
    @Metadata(required = false)
    private String schemaName;

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String name) {
        this.wallet = name;
    }

    public String getService() {
        return service;
    }

    public void setService(String path) {
        this.service = path;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
}
