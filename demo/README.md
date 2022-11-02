# Nessus Aries Demo

In this demo we show how Faber College uses Camel Aries to fetch data from a source and issue verifieable credentials from it.
In it initial POC, Faber uses Google Sheets as data source.

## Setup and test the Google Sheets API 

First, we have to go through some non-trivial process to setup OAUTH for our Google Sheet - this is documented [here](./docs/google-access.md)

    export GOOGLE_OAUTH_CLIENT_ID=***.apps.googleusercontent.com
    export GOOGLE_OAUTH_CLIENT_SECRET=***
    export GOOGLE_OAUTH_REFRESH_TOKEN=***
    
    GOOGLE_OAUTH_ACCESS_TOKEN=$(curl -sX POST https://oauth2.googleapis.com/token \
       -d "client_id=${GOOGLE_OAUTH_CLIENT_ID}&client_secret=${GOOGLE_OAUTH_CLIENT_SECRET}&refresh_token=${GOOGLE_OAUTH_REFRESH_TOKEN}&grant_type=refresh_token" | jq .access_token) \
       && echo "GOOGLE_OAUTH_ACCESS_TOKEN=${GOOGLE_OAUTH_ACCESS_TOKEN}"

    export GOOGLE_SPREADSHEET_ID=1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw
    
    curl https://sheets.googleapis.com/v4/spreadsheets/${GOOGLE_SPREADSHEET_ID}/values/a1:h \
      --header "Authorization: Bearer ${GOOGLE_OAUTH_ACCESS_TOKEN}"

## Access the Google Sheet

For the demo, Faber [opens the sheet](https://docs.google.com/spreadsheets/d/1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw/edit) and resets all status flags to 0

## Build and run the demo image

    $ mvn clean install -Pdemo -pl demo -am -DskipTests
    ...
    [INFO] --- docker-maven-plugin:0.40.1:build (docker-build) @ nessus-aries-demo ---
    [INFO] Building tar: /Users/tdiesler/git/nessus-aries/demo/target/docker/nessusio/aries-demo/tmp/docker-build.tar
    [INFO] DOCKER> [nessusio/aries-demo:latest]: Created docker-build.tar in 299 milliseconds
    [INFO] DOCKER> [nessusio/aries-demo:latest]: Built image sha256:d7330
    [INFO] DOCKER> [nessusio/aries-demo:latest]: Tag with latest,0.1.0-SNAPSHOT
    
    docker run -it --rm \
        -e GOOGLE_OAUTH_CLIENT_ID=${GOOGLE_OAUTH_CLIENT_ID} \
        -e GOOGLE_OAUTH_CLIENT_SECRET=${GOOGLE_OAUTH_CLIENT_SECRET} \
        -e GOOGLE_OAUTH_REFRESH_TOKEN=${GOOGLE_OAUTH_REFRESH_TOKEN} \
        -e INDY_WEBSERVER_HOSTNAME=host.docker.internal \
        -e ACAPY_HOSTNAME=host.docker.internal \
        nessusio/aries-demo \
            --spreadsheet-id=1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw

    2022-06-22 09:34:31 INFO  - Onboard Faber ==========================================================================================================
    2022-06-22 09:34:31 INFO  - CreateWalletRequest: {"wallet_dispatch_type":"default","wallet_key":"FaberKey","wallet_name":"Faber","wallet_type":"indy"}
    2022-06-22 09:34:32 INFO  - Faber: DID(did=YP6NGh88tUKphTuuVxPanr, keyType=ed25519, method=sov, posture=wallet_only, verkey=J71iMT8mXbkEVJGbymuejgFbYja52sdF5Cd7Mx474eg3)
    2022-06-22 09:34:32 INFO  - Self register: {"did":"YP6NGh88tUKphTuuVxPanr","verkey":"J71iMT8mXbkEVJGbymuejgFbYja52sdF5Cd7Mx474eg3","alias":"Faber","role":"ENDORSER"}
    2022-06-22 09:34:32 INFO  - Respose: {"did":"YP6NGh88tUKphTuuVxPanr","seed":null,"verkey":"J71iMT8mXbkEVJGbymuejgFbYja52sdF5Cd7Mx474eg3"}
    2022-06-22 09:34:36 INFO  - Apache Camel 3.17.1-SNAPSHOT (camel-1) is starting
    2022-06-22 09:34:36 INFO  - Routes startup (total:4 started:4)
    2022-06-22 09:34:36 INFO  -     Started route1 (google-sheets://data/get)
    2022-06-22 09:34:36 INFO  -     Started route2 (direct://processHeaderRow)
    2022-06-22 09:34:36 INFO  -     Started route3 (direct://create-credential-definition)
    2022-06-22 09:34:36 INFO  -     Started route4 (direct://processDataRow)
    2022-06-22 09:34:36 INFO  - Apache Camel 3.17.1-SNAPSHOT (camel-1) started in 376ms (build:98ms init:248ms start:30ms)
    2022-06-22 09:34:38 INFO  - Create Credential Definition ...
    2022-06-22 09:34:38 INFO  - Created Schema: SchemaSendResponse(schemaId=YP6NGh88tUKphTuuVxPanr:2:Transscript:1.2, ..., attrNames=[last_name, year, first_name, average, degree, status, ssn]))
    2022-06-22 09:34:43 INFO  - CredentialDefinitionId: YP6NGh88tUKphTuuVxPanr:3:CL:683:default
    
## Generate Transcript credentials

Faber sets the status flag to 1 for each Transcipt credential it want to issue

    2022-06-22 09:38:44 INFO  - *******************************************************
    2022-06-22 09:38:44 INFO  - *
    2022-06-22 09:38:44 INFO  - * SchemaId:    YP6NGh88tUKphTuuVxPanr:2:Transscript:1.2
    2022-06-22 09:38:44 INFO  - * CredDefId:   YP6NGh88tUKphTuuVxPanr:3:CL:683:default
    2022-06-22 09:38:44 INFO  - * Type:        did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/issue-credential/1.0/offer-credential
    2022-06-22 09:38:44 INFO  - *
    2022-06-22 09:38:44 INFO  - * first_name:  Alice
    2022-06-22 09:38:44 INFO  - * last_name:   Garcia
    2022-06-22 09:38:44 INFO  - * ssn:         123-45-0001
    2022-06-22 09:38:44 INFO  - * degree:      Bachelor of Science Marketing
    2022-06-22 09:38:44 INFO  - * status:      1
    2022-06-22 09:38:44 INFO  - * year:        2015
    2022-06-22 09:38:44 INFO  - * average:     4
    2022-06-22 09:38:44 INFO  - *
    2022-06-22 09:38:44 INFO  - *******************************************************

    