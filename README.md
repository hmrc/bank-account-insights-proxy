# Bank Account Insights Proxy

The `bank-account-insights-proxy` service is a `protected` zone microservice used to forward insights requests from MDTP to CIP in the `private` zone. It runs in `development`, `qa`, `staging`, `externaltest` and `production`. 

In  `development` and `externaltest` the downstream connection is to a [stub](https://github.com/hmrc/bank-account-insights-stub) instead of the `bank-account-insights` service, in order to private known test data to the API platform sandbox.

This service is the entry point to CIP bank account attribute insights services for internal consumers, both on MDTP and through the HMRC coroporate tier. In both cases, an `interal-auth` token is required to access the service.

## Endpoints

The following endpoint(s) are available:

* [https://bank-account-proxy.protected.mdtp/check/insights](https://github.com/hmrc/bank-account-gateway/tree/main/public/api/conf/1.0/docs/insights/insights.md)

## Documentation

Consumer facing documentation for all `bank-account` attribute services - including details of available endpoints - can be found [here](https://github.com/hmrc/bank-account-gateway). These services are also available on the API platform through [bank-account-gateway](https://github.com/hmrc/bank-account-gateway)

## Contact

Our preferred contact method is our public channel in HMRC Digital Slack: `#team-cip-insights-and-reputation`

If you do not have access to Slack, please email us at `cip-insights-and-reputation-g@digital.hmrc.gov.uk`

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
