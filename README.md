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
| MOBILE_CUSTOMER_PROFILE          | sm2 --start MOBILE_CUSTOMER_PROFILE --appendArgs '{"CUSTOMER_PROFILE": ["-DoptInVersionsEnabled='true'", "-DreOptInEnabled='true'", "-DgooglePass.key=ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAic2NhLW5pbm8iLAogICJwcml2YXRlX2tleV9pZCI6ICJiZmUxMjJmZDVkMzY3Y2FjMjQyZDk2YjU5NDMwMWI5YmRjOTZkZWVmIiwKICAicHJpdmF0ZV9rZXkiOiAiLS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tXG5NSUlFdXdJQkFEQU5CZ2txaGtpRzl3MEJBUUVGQUFTQ0JLVXdnZ1NoQWdFQUFvSUJBUURGQVVscTVTMXJ4dk9GXG5pTkJxMGRWQnVKN2FJWU1ubGN1cFU0czNpWFBwZUJITmhxZW4wY3JCSE1NaWhlcUhrRTFWVjV5VGhFdllZSHpzXG5IeGd1dWFhb285RmJ6WHAybzBHaXcvNnVBL2F2eUYrWGQyb3RMbGpQN0tFNGswKzBmamZqNlZ4b0tQUkhUMGd6XG5vSE16aENvNnVOV2xaMWFCd3NuWlVVekJRck4zNlhsS29NRGVhWEFNdm8zQS93c3NDNFJmYW9ZNWROdmZFUnlyXG5nUUF0RmlSWXpQelI0VS9ueGFVWGNXeXFOa3U5eG9wb0NwR0NVbFovcHRMdEhuMHBrOSt6c1JjSGlFMG9oSGRZXG5wUUphaGtjS3AyNDBJalcwWDdNTkFEdXg5L3NPTkpETWVYTDEwS0UvNTZFZFBzOWFKWTJmSUtwUDU5c2M5QTNhXG42VXBNdzdtbkFnTUJBQUVDZ2Y4c2QrY0lURVVyUmFyN2k2OS8ydDEvTHhyMWF3alVacUEycXJkQ0NOY083bWJOXG5OSVdqa3NteGhFeUh0VVVFVktDTjJkMXJ1Qld2MUJmTVdUaDNuTG1CeFM5cWR5NGJLNzYzV2owMDVkUzIyUVBmXG5ZNVVUNXVKSnc2QU9YQTJQZVlCdFRRakZuc0lYVnBjc2ZkUmVsYTJFM09yS2ZYYXQrRGU4VmgyaHl6VzNmNmhqXG40TjdoUk1FTllZVFpSTXVlUjRnZzlwdkV0dEhlZndiYVNOMjR4MGIyMkpRWWFBTG9NZmhvczVYcEVlS2s1eFJaXG5BMXZmZXZiUmVoT285UFQwRnJWUS9XdytyQUlyelowU2VOdkFFT3o2Qk1kVVZOWWRXQlFVVzlwNU9RY3dTVS9wXG5xQWxaVGVBSTJETnJiTE5FS01LRlFKeEUrVzI1YTBvQ0RCZ0pDQ1VDZ1lFQTl4WGZWdHgvczZGU0cxRHNtWjBmXG5VcFBEVFRSaGNwWi82UkRLWDBzZGZmRWxMMnZ6N0pKQStkLzV2cmNHNm5DVjdWdzVmREVwNHdrR3JEWmNHNkpmXG5RbjhzYzgrRTY2UTJ4c2gvSUhvQ1FqK3ZXRCtIOXpIZndWNkkwNE9zekR2S0VQNnNNUk9TY21PUHhIdUZQRUk4XG5TUE9yZ0w2dUlnd2hxUUtxcFVPVWtIMENnWUVBekJ6Y3ZRazFGbk1OaEJja3lxbDlGNThPbU5McU14NnB0bFNxXG5sWFpmMEYyZHphaHljL3V1KzNLRGFXdGdZa0NGc2NWNUphVEhFNFprZmhQMXduQ0pWaHc1TnVmWndTVHdxVFJSXG5PYy9ES1A1Nm1BMXVLRWdidWdLWjRTdTl1aUczRXdRTlp6TlRCbkFZeS9TMWtBaGxNbmdqNDlxVW5QalFiNXcyXG5tVWRJVC9NQ2dZQmpDOHFHaUVkTW00dE5WZWd5UDlEUmlsZ010OFdrYUg0SDBHby9QdDRvb1NUMTJJRmtRTkI1XG5HZmFFSTl4SzJDelJoRm1xMWc1amF6Zllpc1hyY2ZCYnVKejZJNksxenhNQ0psY0hqc0VmQzJaZnFyLzNNRy93XG5sTk9tYmk4emczZ2h6ZVQ2bTB6bU85RGl3MTBLWmNiQ3U2THhMZjZodVZrNDVjL0FCZElsQlFLQmdRQ1FTWGJoXG5TcmowZmJCNFI4UUNYMzNHVFBJTXBreFlocnlCMzZnV3IrOWJaRkpCSjJxQkF0SFhma1BYS2NpZ3Erdldsem5rXG5tbThBSm1pemwzaUxVdkpDcFFEdEIwaXZlR1dIdHl3VUtnSlQ0RkRaVytVYkpKNDFCOUd2a3pRemQ5SHE1MXB5XG5NWjNuVnlhd1J2UnlOUDBVaUVrV3NWV05BWGFXNzE3SlM5S2FjUUtCZ0RNSk5OdjN3Tmpla0lPWGJYaitxb1NCXG5EbDlsTlR3dll4c2hia2Zub0M3SENoR05hVm9aTmdVSjRBNXdBbDROdUQ2b3RmZk1ZMG5DeXBVU0lRQThONkVBXG5UZmpQa3RFeFFlQ3RuUXBjd1I1REtYRUtMREpLNmY2V1RUWXdPNjlFc0RyLzlYVWg0UWVmUjBtQ3greVdrRWR4XG5KL291STUzdUtNWU1PUHFzMjBVc1xuLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLVxuIiwKICAiY2xpZW50X2VtYWlsIjogInByaXZhdGUtcGFzc2VzLXdlYi1jbGllbnRAc2NhLW5pbm8uaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLAogICJjbGllbnRfaWQiOiAiMTE3Nzk5MDA0NzQ3MDQxNTUzMDMyIiwKICAiYXV0aF91cmkiOiAiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGgiLAogICJ0b2tlbl91cmkiOiAiaHR0cHM6Ly9vYXV0aDIuZ29vZ2xlYXBpcy5jb20vdG9rZW4iLAogICJhdXRoX3Byb3ZpZGVyX3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vb2F1dGgyL3YxL2NlcnRzIiwKICAiY2xpZW50X3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9wcml2YXRlLXBhc3Nlcy13ZWItY2xpZW50JTQwc2NhLW5pbm8uaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLAogICJ1bml2ZXJzZV9kb21haW4iOiAiZ29vZ2xlYXBpcy5jb20iCn0K"], "PREFERENCES": ["-DfeatureFlag.switchOn='true'"]}'                                                                              |


## Run Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `sbt clean compile coverage test it:test coverageReport dependencyUpdates`


API
---

| *Task*                                               | *Supported Methods* | *Description*                                                                                                        |
|------------------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------|
| ```/profile/personal-details/:nino```                | GET                 | Returns a user's designatory details. [More...](docs/personalDetails.md)                                             |
| ```/profile/preferences```                           | GET                 | Returns the user's preferences. [More...](docs/preferences.md)                                                       |
| ```/profile/preferences/paperless-settings/opt-in``` | POST                | Sets or updates the user's paperless opt-in preference settings. [More...](docs/paperlessSettingsOptIn.md)           |
| ```/profile/preferences/paperless-settings/opt-out``` | POST                | Opts the user out of paperless. [More...](docs/paperlessSettingsOptOut.md)                                           |
| ```/profile/pending-email```                         | POST                | Updates the user's email address for pending emails. [More...](docs/pendingEmail.md)                                 |
| ```/apple-pass```                                    | GET                 | Returns an encrypted pass that allows a user to store a nino in an apple wallet. [More...](docs/getApplePass.md)     |
| ```/google-pass```                                   | GET                 | Returns a JWT token that allows a user to store a nino in an google wallet. [More...](docs/getGooglePass.md) |

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



# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
