# syntax=docker/dockerfile:1
# Keycloak with the email-code authenticator (#83 part 3, ADR 0010). Keycloak 26.5 has no
# built-in email OTP; the plugin is pinned by version and checksum.
FROM quay.io/keycloak/keycloak:26.5.7

ADD --chmod=644 \
    --checksum=sha256:39e887342070a732a8e5c2db9da451d6342fdbf602f37b0534b615298711fd9e \
    https://github.com/for-keycloak/email-otp-authenticator/releases/download/v1.7.0/email-otp-authenticator-v1.7.0-kc-26.5.7.jar \
    /opt/keycloak/providers/email-otp-authenticator.jar
