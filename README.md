# DT-Lambda
AWS Lambda functions with New Relic's open tracing implementation and distributed tracing.

# Discussion of New Relic's Java Lambda Solution For Distributed Tracing

## What is AWS Lambda?
- AWS's serverless solution (i.e. they are functions you can deploy to the cloud with autoscaling)
- These functions take in an event input and return some output.
- Can be used as HTTP handlers via using AWS's API gateway to forward HTTP requests to a lambda solution
- Lambda Layers can be added into your functions to add additional functionality using telemetry

## Issue: Distributed Tracing Failed with Lambda
- Only happened with V2 HTTP API Gateways with AWS.
- Has to do with event payloads being formatted differently for v2 APIs compared to v1 APIs

Example v1 payload snippet for Python:
```
{
      "resource": "/",
      "path": "/",
      "httpMethod": "GET",
      "requestContext": {
          "resourcePath": "/",
          "httpMethod": "GET",
          "path": "/Prod/",
          ...
}
```

Example v2 payload snippet for Python:
```
    "requestContext": {
        "accountId": "123456789012",
        "apiId": "r3pmxmplak",
        "domainName": "r3pmxmplak.execute-api.us-east-2.amazonaws.com",
        "domainPrefix": "r3pmxmplak",
        "http": {
            "method": "GET",
            "path": "/default/nodejs-apig-function-1G3XMPLZXVXYI",
            "protocol": "HTTP/1.1",
            "sourceIp": "205.255.255.176",
            "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
         }
        ...
}
```

Simply updating how the payloads are parsed resolves the issue for python's lambda layer.

## How the Java lambda solution differs?

- Python's lambda layer used the python agent
- Java instead uses an open tracing implementation
- Open tracing is a tracing API standard for creating spans and distributed traces that predated Open Telemetry.
- Turns out distributed tracing worked fot the Java lambda solution. Unlike for Python, the event payloads were the same.
- You can set up the lamnda solution via adding a lambda layer as shown in the links below:
    - [How to install a lambda layer](https://docs.newrelic.com/docs/serverless-function-monitoring/aws-lambda-monitoring/instrument-lambda-function/instrument-your-own/#console)
    - [List of lambda layers](https://layers.newrelic-external.com/)
- You can also use our open tracing implementation directly as shown in it's [github repo readme](https://github.com/newrelic/newrelic-lambda-tracer-java?tab=readme-ov-file#usage)


## Demo Time

- We use 2 lambda functions.
- We have a front end function with an API gateway that forwards requests to it
- We also have a backend function that with both v1 and v2 HTTP API gateways that forwards requests to it
- The front end function calls the backend functions with HTTP calls while directly using the Open tracing API to pass distributed tracing headers into the HTTP payloads

Time to demonstrate!