
# customs-financials-email-throttler

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Coverage](https://img.shields.io/badge/test_coverage-90-green.svg)](/target/scala-3.3.5/scoverage-report/index.html) [![Accessibility](https://img.shields.io/badge/WCAG2.2-AA-purple.svg)](https://www.gov.uk/service-manual/helping-people-to-use-your-service/understanding-wcag)

A backend service to control the rate at which emails are dispatched.

This application lives in the "public" zone.

## Running the service

*From the root directory*

`sbt run` - starts the service locally

`sbt runAllChecks` - Will run all checks required for a successful build

### Required dependencies

There are a number of dependencies required to run the service.

The easiest way to get started with these is via the service manager CLI - you can find the installation guide [here](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html)

| Command                                         | Description                                                      |
|-------------------------------------------------|------------------------------------------------------------------|
| `sm2 --start CUSTOMS_FINANCIALS_ALL`            | Runs all dependencies                                            |
| `sm2 --start CUSTOMS_EMAIL_FRONTEND_ALL `       | Runs all email dependencies                                      |
| `sm2 -s`                                        | Shows running services                                           |
| `sm2 --stop CUSTOMS_FINANCIALS_EMAIL_THROTTLER` | Stop the micro service                                           |
| `sbt run` `                                     | (from root dir) to compile the current service with your changes |

### Runtime Dependencies
(These are subject to change and may not include every dependency)

* `AUTH`
* `AUTH_LOGIN_STUB`
* `AUTH_LOGIN_API`
* `BAS_GATEWAY`
* `CA_FRONTEND`
* `SSO`
* `USER_DETAILS`
* `EMAIL`
* `CONTACT_FRONTEND`

## Testing

The minimum requirement for test coverage is 90%. Builds will fail when the project drops below this threshold.

### Unit Tests

| Command                                | Description                  |
|----------------------------------------|------------------------------|
| `sbt test`                             | Runs unit tests locally      |
| `sbt "test/testOnly *TEST_FILE_NAME*"` | runs tests for a single file |

### Coverage

| Command                                  | Description                                                                                                 |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `sbt clean coverage test coverageReport` | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |

## Available Routes

You can find a list of microservice specific routes here - `/conf/app.routes`

### POST /enqueue-email

#### Request Body
```json 
{
  "to": [
    "email1@example.co.uk",
    "email2@example.co.uk"
  ],
    "templateId": "$id",
    "parameters": {
    "param1": "value1",
    "param2": "value2"
  },
    "force": false,
    "enrolment": "testEori",
    "eventUrl": "event.url.co.uk",
    "onSendUrl": "on.send.url.co.uk"
}
```

#### Response Body
```json
{
    "Status": "Ok",
    "message": "Email successfully queued"
}
```

#### Response code specification:
* **202** If the request is processed successful
* **400** This status code will be returned in case of incorrect data, incorrect data format, missing parameters etc are provided in the request

## Feature Switches
Not applicable

## Helpful commands

| Command                                       | Description                                                                                                 |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `sbt runAllChecks`                            | Runs all standard code checks                                                                               |
| `sbt clean`                                   | Cleans code                                                                                                 |
| `sbt compile`                                 | Better to say 'Compiles the code'                                                                           |
| `sbt coverage`                                | Prints code coverage                                                                                        |
| `sbt test`                                    | Runs unit tests                                                                                             |
| `sbt it/test`                                 | Runs integration tests                                                                                      |
| `sbt scalafmtCheckAll`                        | Runs code formatting checks based on .scalafmt.conf                                                         |
| `sbt scalastyle`                              | Runs code style checks based on /scalastyle-config.xml                                                      |
| `sbt Test/scalastyle`                         | Runs code style checks for unit test code /test-scalastyle-config.xml                                       |
| `sbt coverageReport`                          | Produces a code coverage report                                                                             |
| `sbt "test/testOnly *TEST_FILE_NAME*"`        | runs tests for a single file                                                                                |
| `sbt clean coverage test coverageReport`      | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |
| `sbt "run -Dfeatures.some-feature-name=true"` | enables a feature locally without risking exposure                                                          |

