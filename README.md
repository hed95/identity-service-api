![Identity Service API Test and Build](https://github.com/DigitalPatterns/identity-service-api/workflows/Identity%20Service%20API%20Test%20and%20Build/badge.svg)

# Identity Service API


An api for storing both MRZ scans and biometric information.

This API is fully secured and stores the data in S3.

### Environment

#### Bootstrap configuration

The following environment variables are required to load properties from AWS secrets manager

* AWS_SECRETS_MANAGER_ENABLED
* AWS_REGION
* AWS_ACCESS_KEY
* AWS_SECRET_KEY
* SPRING_PROFILES_ACTIVE

```json
{
  "api.allowedAudiences": "x",
  "auth.url": "https://keycloak.lodev",
  "auth.realm": "elf",
  "api.read.roles": "read-role, read-role2",
  "api.update.roles": "update-role, update-role2",
  "aws.s3.identity.scanData": "scans"
}
```

***Audience is required and checked. If the audience do not match a 403 will be returned***
***If read roles not configured then the caller will get a 403***
***If update roles not configured then the caller will get a 403***

Both roles need to be present in order to perform an update.

Only read role required for read.

Swagger UI:

```
{server address}/swagger/ui.html
```