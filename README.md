
# customs-financials-email-throttler

A backend service to control the rate at which emails are dispatched.

This application lives in the "public" zone.

## Running the application locally

The service has the following dependencies:

* `AUTH`
* `AUTH_LOGIN_STUB`
* `AUTH_LOGIN_API`
* `USER_DETAILS`
* `EMAIL`
* `CONTACT_FRONTEND`

You can use the CUSTOMS_EMAIL_THROTTLER profile in service manager to start these services.

Once these services are running, you should be able to do `sbt "run 9872"` to start in `DEV` mode.

## Running tests

There is just one test source tree in the `test` folder. Use `sbt test` to run them.

To get a unit test coverage report, you can run `sbt clean coverage test coverageReport`,
then open the resulting coverage report `target/scala-2.12/scoverage-report/index.html` in a web browser.

The test coverage threshold is currently set at 85%. Any significant commits of code without corresponding tests may result in the build failing.

## All tests and checks

This is a sbt command alias specific to this project. It will run a scala style check, run unit tests, run integration
tests and produce a coverage report:

> `sbt runAllChecks`
