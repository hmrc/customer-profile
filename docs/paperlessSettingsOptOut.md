Paperless Settings Opt Out
----
  Opts the user out of paperless

* **URL**

  `/profile/preferences/paperless-settings/opt-out`

* **Method:**

  `POST`

*  **URL Params**

  **Required:**
     `journeyId=[String]`
  
     a string which is included for journey tracking purposes but has no functional impact
        
*  **Request body**

```json
{ 
"generic": { 
    "accepted": false,
    "optInPage": { 
        "cohort": 24,
        "pageType": "AndroidOptOutPage", 
        "version": { 
            "major": 1, 
            "minor": 2 
        } 
    }
},
"language": "en" 
}
```

* **Success Response:**

  * **Code:** 200 <br />
    **Description:** The opt-out preference has been updated

* **Error Response:**

  * **Code:** 400 BAD REQUEST<br />
    **Content:** `{"code":"BAD_REQUEST","message":"JSON error flattened to a string describing the error that occured on the request"}`

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{"code":"UNAUTHORIZED","message":"Some auth message"}`

  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{"code":"LOW_CONFIDENCE_LEVEL","Confidence Level on account does not allow access"}`

  * **Code:** 404 NOT FOUND <br />
    **Content:** `{"code":"NOT_FOUND","message":"No record to set opt-out preference against"}`

  * **Code:** 406 NOT ACCEPTABLE <br />
    **Content:** `{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}`

  * **Code:** 500 INTERNAL_SERVER_ERROR <br />


