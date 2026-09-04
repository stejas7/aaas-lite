# AAAS Lite — OIDC login and verified access

Gradle multi-project starter for Cognito-backed OIDC login, identity linking through ODL, and protected access through ACE.

![Java](https://img.shields.io/badge/Java-17-17365d) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6db33f) ![Build](https://github.com/stejas7/aaas-lite/actions/workflows/ci.yml/badge.svg)

## What is included

- `login-web`: responsive login/onboarding page, Spring Security OAuth2 login and server-side token handling.
- `ace-service`: Cognito access-token validation, OIDC identity-resolution orchestration and fail-closed authorization.
- `identity-core`: framework-independent approved-triplet and access-policy rules.
- `infrastructure/cognito.yaml`: Cognito User Pool, confidential web app client, optional Ping-compatible OIDC provider, short token lifetimes and lifecycle initialization triggers.
- Tests for all four triplets, invalid PII input, ID-token rejection, wrong-client rejection, stale ODL state, unavailable ODL, terminal deletion ordering, timeouts and correlation blocking.

The repository is an integration starter. It does not include enterprise Ping or ODL URLs, secrets, proprietary MFA, domain records or a production relying application. See [architecture and integration gaps](docs/architecture.md).

## Run the login page locally

Local mode intentionally renders the finished page with login disabled and denies all protected endpoints. It needs no AWS account.

```bash
gradle :login-web:bootRun
```

Open <http://localhost:8080>. With Docker:

```bash
docker build --build-arg MODULE=login-web -t aaas-login-web .
docker run --rm -p 8080:8080 aaas-login-web
```

## Configure Cognito and OIDC

Review the template first. Deployment creates billable AWS resources and requires a trusted OIDC client secret. Store parameters in an encrypted deployment system; never commit a parameter file containing the secret.

```bash
aws cloudformation deploy   --template-file infrastructure/cognito.yaml   --stack-name aaas-lite-dev   --capabilities CAPABILITY_IAM   --region ap-south-1   --parameter-overrides     DomainPrefix=replace-with-unique-prefix     OidcProviderName=PingFederate     OidcClientId=replace     OidcClientSecret=replace     OidcIssuer=https://replace.example.com     OidcAuthorizeUrl=https://replace.example.com/as/authorization.oauth2     OidcTokenUrl=https://replace.example.com/as/token.oauth2     OidcUserInfoUrl=https://replace.example.com/idp/userinfo.openid     OidcJwksUrl=https://replace.example.com/pf/JWKS
```

CloudFormation returns the User Pool ID, web Client ID, issuer and managed-login base URL. Retrieve the generated app-client secret through an authorized deployment process; do not print it into shared logs.

## Run live mode

Login Web:

```bash
SPRING_PROFILES_ACTIVE=live COGNITO_CLIENT_ID=replace COGNITO_CLIENT_SECRET=replace COGNITO_ISSUER_URI=https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_REPLACE COGNITO_IDENTITY_PROVIDER=PingFederate ACE_BASE_URL=https://ace.internal.example gradle :login-web:bootRun
```

ACE needs an AWS runtime role allowed to call `cognito-idp:GetUser`, `AdminGetUser`, `AdminUpdateUserAttributes` and `AdminDeleteUser` only on this pool. `GetUser` is authorized by the user's access token; lifecycle writes/deletion use the role. ODL requires HTTPS and a server-side service token in this starter.

```bash
SPRING_PROFILES_ACTIVE=live COGNITO_REGION=ap-south-1 COGNITO_POOL_ID=ap-south-1_REPLACE COGNITO_CLIENT_ID=replace COGNITO_ISSUER_URI=https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_REPLACE ODL_BASE_URL=https://odl.internal.example ODL_SERVICE_TOKEN=replace gradle :ace-service:bootRun
```

Do not place real SSN fragments, employee IDs, dates, tokens or provider secrets in source, screenshots, CI variables visible to pull requests, command history or application logs.

Registration lifecycle writes and deletion are disabled by default. Enable `REGISTRATION_ENABLED=true` only after the ODL adapter contract has been implemented and reviewed, including immutable terminal decisions, idempotent operations and recovery. Login and protected access checks do not require this flag. For local HTTP live testing only, use `SESSION_COOKIE_SECURE=false`; keep the default `true` with HTTPS in deployed environments. Configure `COGNITO_REDIRECT_URI` to match the registered callback exactly.

## Build and test

Use Java 17 and Gradle 8.14.4+:

```bash
gradle clean build
```

GitHub Actions has successfully compiled both services, built their executable JARs, and run the test suite. See the [successful build](https://github.com/stejas7/aaas-lite/actions/runs/33863466438). Live Ping/Cognito/ODL integration still requires a configured test environment.

## Primary references

- [Amazon Cognito authentication](https://docs.aws.amazon.com/cognito/latest/developerguide/authentication.html)
- [Cognito OIDC identity providers](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-oidc-idp.html)
- [Cognito Lambda triggers](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-working-with-lambda-triggers.html)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Spring Security JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Boot Gradle plugin](https://docs.spring.io/spring-boot/gradle-plugin/index.html)
