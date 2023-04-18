Get Apple Pass
----
Retrieves a base 64 encoded pass that can be called from the HMRC mobile app to allow a user to add their nino to an Apple Wallet.

The acceptance is based off the user being shown and agreeing to, these [Terms and Conditions](https://www.tax.service.gov.uk/information/terms#secure)

* **URL**

  `/apple-pass`

* **Method:**

  `GET`

*  **URL Params**

**Required:**
`journeyId=[String]`

a string which is included for journey tracking purposes but has no functional impact

*  **Request body**

```json
{ 
"applePass": "UEsDBBQACAgIABxqJlYAAAAAAAAAAAAAAAAIAAAAaWNvbi5wbmcBYjCdz4lQTkcNChoKAAAADUlIRFIAAAC0"
}
```

* **Success Response:**

    * **Code:** 200 <br />
      **Description:** Sucessfully updated

* **Error Response:**

    * **Code:** 400 BAD REQUEST<br />
      **Content:** `{"code":"BAD_REQUEST","message":"JSON error flattened to a string describing the error that occured on the request"}`

    * **Code:** 401 UNAUTHORIZED <br />
      **Content:** `{"code":"UNAUTHORIZED","message":"Some auth error message"}`

    * **Code:** 403 FORBIDDEN <br />
      **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

    * **Code:** 404 NOT FOUND <br />
      **Content:** `{"code":"NOT_FOUND","Resource was not found"}`
      **Note:** Propagates a NOT_FOUND from the preferences system

    * **Code:** 406 NOT ACCEPTABLE <br />
      **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

    * **Code:** 409 CONFLICT <br />
      **Content:** `{"code":"CONFLICT","message":"No existing verified or pending data"}`

    * **Code:** 500 INTERNAL_SERVER_ERROR <br />


