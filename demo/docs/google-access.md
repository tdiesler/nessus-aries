## Google API

Email: fuse.wfc.ci@gmail.com

https://developers.google.com/sheets/api/guides/authorizing

1) Go to the [API Console](https://console.cloud.google.com)
2) Select or create a project (e.g. Nessus Aries 001)
3) Select Google Sheets API and click Enable
4) Click Credentials, then select OAuth client ID
5) If this is your first time creating a client ID, you must also configure your Consent Screen
    Application name: Faber Demo App
    Support email: fuse.wfc.ci@gmail.com
    Test user: fuse.wfc.ci@gmail.com
    Scopes for Google APIs
        https://www.googleapis.com/auth/spreadsheets
6) Create OAuth client ID
    Application type: Web application
    Name: Faber Demo Client
    Authorized redirect URIs: https://developers.google.com/oauthplayground
7) [Authorize APIs](https://developers.google.com/oauthplayground)
    Setting | Use your own OAuth credentials
    
    Sheets API v4 `https://www.googleapis.com/auth/spreadsheets`
    
    export GOOGLE_OAUTH_CLIENT_ID=***.apps.googleusercontent.com
    export GOOGLE_OAUTH_CLIENT_SECRET=***
    export GOOGLE_OAUTH_REFRESH_TOKEN=***

8) Refresh access token 

GOOGLE_OAUTH_ACCESS_TOKEN=$(curl -sX POST https://oauth2.googleapis.com/token \
   -d "client_id=${GOOGLE_OAUTH_CLIENT_ID}&client_secret=${GOOGLE_OAUTH_CLIENT_SECRET}&refresh_token=${GOOGLE_OAUTH_REFRESH_TOKEN}&grant_type=refresh_token" | jq .access_token) \
   && echo "GOOGLE_OAUTH_ACCESS_TOKEN=${GOOGLE_OAUTH_ACCESS_TOKEN}"

Test Spreadsheet access 

    export GOOGLE_SPREADSHEET_ID=1D2RogwD1LgsNC9rRc7JET1aXUPIyORP57QQO6lNb2nw
    
    curl https://sheets.googleapis.com/v4/spreadsheets/${GOOGLE_SPREADSHEET_ID}/values/a1:h \
      --header "Authorization: Bearer ${GOOGLE_OAUTH_ACCESS_TOKEN}"
