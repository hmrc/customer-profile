# CUSTOMER-PROFILE

[![Build Status](https://travis-ci.org/hmrc/customer-profile.svg?branch=master)](https://travis-ci.org/hmrc/customer-profile) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customer-profile/images/download.svg) ](https://bintray.com/hmrc/releases/customer-profile/_latestVersion)

Allows users to view tax profile and communication preferences
 

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```.

## Development Setup
- Run locally: `sbt run` which runs on port `8233` by default
- Run with test endpoints: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The service can be run locally from Service Manager, using the following profiles:

| Profile Details                  | Command                                                                                                           |
|----------------------------------|:------------------------------------------------------------------------------------------------------------------|
| MOBILE_CUSTOMER_PROFILE          | sm2 --start MOBILE_CUSTOMER_PROFILE                                                                    |


## Run Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `sbt clean compile coverage test it:test coverageReport dependencyUpdates`


API
---

| *Task*                         | *Supported Methods* | *Description*                                                                                                    |
|--------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------|
| ```/profile/personal-details/:nino``` | GET                 | Returns a user's designatory details. [More...](docs/personalDetails.md)                                         |
| ```/profile/preferences```     | GET                 | Returns the user's preferences. [More...](docs/preferences.md)                                                   |
| ```/profile/preferences/paperless-settings/opt-in``` | POST                | Sets or updates the user's paperless opt-in preference settings. [More...](docs/paperlessSettingsOptIn.md)       |
| ```/profile/preferences/paperless-settings/opt-out``` | POST                | Opts the user out of paperless. [More...](docs/paperlessSettingsOptOut.md)                                       |
| ```/profile/pending-email```   | POST                | Updates the user's email address for pending emails. [More...](docs/pendingEmail.md)                             |
| ```/apple-pass```              | GET                 | Returns an encrypted pass that allows a user to store a nino in an apple wallet. [More...](docs/getApplePass.md) |
| ```/google-pass```             | GET                 | Returns a JWT token that allows a user to store a nino in an google wallet. [More...](docs/getGooglePass.md)     |
| ```/validate/pin/:enteredPin``` | GET                 | Validate the PIN against DOB pattern and previously used pins.                                                   
| ```/upsert```       | PUT                 | Update/Insert PIN in the DB.                                                                                     |


### `GET /validate/pin/:enteredPin`

Validate the entered pin against the DOB pattern and previously used pin

- Local testing URL - http://localhost:8233/validate/pin/{enteredPin}?journeyId=<journeyId>&deviceId=<deviceId>
- QA URL - https://api.qa.tax.service.gov.uk/customer-profile/validate/pin/{enteredPin}?journeyId=<journeyId>&deviceId=<deviceId>
- Staging URL - https://api.staging.tax.service.gov.uk/customer-profile/validate/pin/{enteredPin}?journeyId=<journeyId>&deviceId=<deviceId>

- Headers     - Accept -> application/vnd.hmrc.1.0+json
- Bearer token needs to be supplied

Example response body for success with 200 OK:
```json
{
   "key" : "valid_pin",
   "message ":  "Pin is valid"
}
```

Example response body for DOB match with 200 OK:
```json
{
    "key" : "create_pin_date_of_birth_error_message",
   "message ":  "PIN should not include your date of birth"
}
```

Example response body for previous pin match 200 OK:
```json
{
    "key" : "change_pin_disallow_previous_pins_error_message",
   "message ":  "Do not re-use an old PIN"
}
```

### `PUT /upsert`

- Local testing URL - http:/localhost:8233/upsert
- QA URL - https://api.qa.tax.service.gov.uk/customer-profile/upsert
- Staging URL - https://api.staging.tax.service.gov.uk/customer-profile/upsert

- Headers     - Accept -> application/vnd.hmrc.1.0+json
- Bearer token needs to be supplied
n
```json
{
    "pin" : "123456",
   "deviceId ": "device-002"
}
```
# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint, e.g.
```
    GET /sandbox/profile/accounts
```


To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" to specify the appropriate status code and return payload. 
See each linked file for details:

| *Task*                                                       | *Supported Methods* | *Description*                                                                                    |
|--------------------------------------------------------------|---------------------|--------------------------------------------------------------------------------------------------|
| ```/sandbox/profile/personal-details/:nino```                | GET                 | Acts as a stub for the related live endpoint. [More...](docs/sandbox/personalDetails.md)         |
| ```/sandbox/profile/preferences```                           | GET                 | Acts as a stub for the related live endpoint. [More...](docs/sandbox/preferences.md)             |
| ```/sandbox/profile/preferences/paperless-settings/opt-in``` | POST                | Acts as a stub for the related live endpoint. [More...](docs/sandbox/paperlessSettingsOptIn.md)  |
| ```/sandbox/profile/preferences/paperless-settings/opt-out``` | POST                | Acts as a stub for the related live endpoint. [More...](docs/sandbox/paperlessSettingsOptOut.md) |
| ```/sandbox/profile/pending-email```                         | POST                | Acts as a stub for the related live endpoint. [More...](docs/pendingEmail.md)                    |
| ```/sandbox/apple-pass```                                    | GET                  | Acts as a stub for the related live endpoint.               |

## Test-Only Endpoints
### `POST       /pin/test-only/save-pin`
save some pin data in DB
```json
{
   "deviceId ": "device-002",
  "ninoHash" : "SZ123456B",
  "hashedPins" : ["123445","567890"]
}
```

### `DELETE       /pin/test-only/delete`
delete all pin data from DB

### `DELETE /pin/test-only/delete/deviceId/:deviceId/nino/:nino`
delete all pin data from DB based on device id and nino

# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
