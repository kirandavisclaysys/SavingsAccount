# Connector for Savings app

This connector adapts the Coastal Savings app tile to the local storage service,
MeridianLink.

This connector depends on an external local storage API
to save the details of the application as they progress through the 
tile. 

This connector makes use of the MeridianLink DecisionXA and BookApp
API for submitting an application to MeridianLink that contains
information drawn from the local storage api and the
CDP container api. 

## Tests

To run the unit tests from the terminal, use: `mvn test`

##  Local Development

Set CONNECTOR_LOCAL=true in your environment variables when testing locally.

A proxy server is included and can be used to overwrite parameters in the request from the tile.
The purpose of this is to overwrite the username so each dev can have their own test data.

Check inside the `proxy` directory for help on running a proxy server.

It will be necessary to set up your proxy with a userId parameter in the userData
that's unique to you, as well as setting up the local storage service's URL
and API key. (In deployment, these URLs and keys are set up as global parameters
and are accessed in our code the same way.)

* `cd` to the `proxy` directory
* run "npm install" to fetch the necessary modules
* load the configuration file (`connector-request-params.json`) in your favorite editor and populate it (example below)
* run the app with `node proxy.js`
* start the tile (make sure the mock_connectorConfig.json file is set up to make connector calls on port 8081)

The proxy app listens for tile connector requests on 8081, updates the request body
with data from the connector-request-params.json file, and sends the request
to the connector.

The app listens for changes to the connector-request-params.json file, so all you have to do to update parameters is to change the file and save.

The params file allows adding/updating connector params and user data.
The format of the params file is:

```
{
  "connectorParameters" : [
    {
      "name": "localStorageUrl", 
      "value": "https://coastaldatadev.cloudhub.io/api"
    },
    {
      "name": "localStorageApiKey", 
      "value": "PUT THE API KEY HERE"
    }
    {
      "name": "bookappApiUrl", 
      "value": "https://beta.loanspq.com/Services/BookApp/BookApp.aspx"
    }
    {
      "name": "bookappApiUserIdBase64", 
      "value": "NzZkNDBhZjMtYmUzMy00OTU5LThhOWQtY2E4Mzg5OWNkMDk3"
    }
    {
      "name": "bookappApiPasswordBase64", 
      "value": "PUT THE API PASSWORD HERE"
    }
  ],
  "userData": {
    "userId": "PUT A UNIQUE ID HERE"
  }
}
```

You can add any parameters you want.
Some of these parameters will be emulating the connector's global parameters
since we can't see those in deployment.

ALL GLOBAL PARAMETERS THAT YOU WOULD NEED WHILE DEPLOYED MUST BE SPECIFIED
HERE WHEN TESTING LOCALLY. 

## Local Storage Database

This connector is dependent upon a database for persisting a user's applications
until they are submitted. It expects global parameters called `localStorageUrl`
and `localStorageApiKey` that can be set on the CDP portal when deployed or set in
the proxy server's configuration for local testing, like you see above.


## Deploying to Constellation Digital Platform 
We have included a script that will build a deploy directory containing the three files that you need to upload and deploy this connector to the CDP.


### CDP Global Parameters
This connector needs the following parameters set up on CDP:
```
localStorageUrl                | url of local storage service for saving application state
localStorageApiKey             | api key for that service

meridianLinkApiUrl             | url for decisionloan endpoint
meridianLinkProductSelectUrl   | url for productselect endpoint (beta/production config)
meridianLinkApiUserIdBase64    | ML credentials
meridianLinkApiPasswordBase64  | ML credentials
bookappApiUrl                  | url for booking the account
bookappApiUserIdBase64         | ML credentials
bookappApiPasswordBase64       | ML credentials
uspsApiUrl                     | https://secure.shippingapis.com/ShippingAPI.dll
uspsApiKey                     | usps API key provided by USPS
eclEnabled                     | TRUE/FALSE  controls whether logging is enabled.

citizenshipStatus1             | US CITIZEN
citizenshipStatus2             | PERMANENTRESIDENT
citizenshipStatus3             | NONPERMRESIDENT
DepositLenderID                | 36ac233dc6a442a088937dee93ba6275
organizationID                 | CFCU120621O
lenderID                       | coastal1
reApplyDays                    | 1
declinedReApplyDays            | 1

```

### CDP Method Parameters

# getDepositRateChanges
depositRateChangesApiurl       |  https://demo.consumer.meridianlink.com/services/ssfcu/ProductRetrieval.aspx


# getProductDetailsDecisionXA
BranchCode                     |  ONLINE_020
meridianLinkApiPasswordBase64  |  SHc3eCleeCleNUJoQDEyLnhQRnp1OUNLcXZIIW9PX3dFcS0sY1NM
meridianLinkApiUrl             |  https://demo.consumer.meridianlink.com/Services/DecisionXA/DecisionXA.aspx

# bookSavingsAccount
bookappApiPasswordBase64       |  SHc3eCleeCleNUJoQDEyLnhQRnp1OUNLcXZIIW9PX3dFcS0sY1NM
bookappApiUserIdBase64         |  MDA1NWRkNzEtZDQ5ZC00ZjA5LWJlN2UtNDdmYzlkNDIyY2Rh
EnableBooking                  |  TRUE
bookappApiUrl                  |  https://demo.consumer.meridianlink.com/Services/BookApp/BookApp.aspx

# getloans
GetLoansApiUrl                 |  https://demo.consumer.meridianlink.com/Services/GetLoans/GetLoans.aspx
getLoansApiUserIdBase64        |  MDA1NWRkNzEtZDQ5ZC00ZjA5LWJlN2UtNDdmYzlkNDIyY2Rh
getLoansApiPasswordBase64      |  SHc3eCleeCleNUJoQDEyLnhQRnp1OUNLcXZIIW9PX3dFcS0sY1NM

# generatePDF
generatePDFApiPasswordBase64   |  SHc3eCleeCleNUJoQDEyLnhQRnp1OUNLcXZIIW9PX3dFcS0sY1NM
generatePDFApiUrl              |  https://demo.consumer.meridianlink.com/services/docs/generatepdfs/generatepdfsaspx
generatePDFApiUserIdBase64     |  MDA1NWRkNzEtZDQ5ZC00ZjA5LWJlN2UtNDdmYzlkNDIyY2Rh
pdfCode                        |  DSCFM0
returnPdf                      |  FALSE
saveTo                         |  ARCHIVED


```

The MeridianLink credentials need to be base64 encoded because they
might contain special characters that the CDP doesn't allow inside of global
parameters.

```

### Logging
Constellation provides a `ConnectorLogging` class that lets connectors
log events to Constellation's platform that you can check on the CU portal.
This connector extends this class with `EnhancedConnectorLogging` that lets
you customize the logging behavior in two ways:
1) The `connector.local` value in your `application.properties` file can be `TRUE` or `FALSE`.
This value can be set to TRUE for local testing. If it's true, the ConnectorLogging is bypassed 

2) The `eclEnabled` global parameter. A deployed connector can be set up with this global parameter
that can be `TRUE` or `FALSE`. Logging is skipped if this is `FALSE` or missing.
This lets you enable/disable ConnectorLogging without redeploying the connector.
 