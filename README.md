# JwtTest

JwtTest is a tiny script that issues a JWT token and also exposes a JWKS endpoint for the verification of the token.
Use it to test your JWT verification code or when you are developing a JWT-secured API locally and need to add a token to your requests. This project uses [JBang](https://www.jbang.dev), which will take care of installing all the dependencies for you.
These dependencies are:

- Java 11+
- [Javalin](https://javalin.io/)
- [Auth0 Java JWT](https://github.com/auth0/java-jwt)
- [Nimbus JOSE + JWT](https://connect2id.com/products/nimbus-jose-jwt)
- [SLF4J](http://www.slf4j.org/) with [slf4j-simple](https://www.slf4j.org/apidocs/org/slf4j/simple/SimpleLogger.html)

## Installation and usage

Install JBang if not already installed:

```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

Then simply start the script:

```bash
jbang run https://github.com/arov00/JwtTest/blob/master/JwtTest.java
```

Alternatively, you can clone this repository and run the script locally (JBang is still required):

```bash
./JwtTest.java
```

You can verify that the JWKS endpoint is working by running:

```bash
curl http://localhost:7000/jwks
```

The default JWT is fine for demonstration purposes, but you probably want to customize it:

```bash
./JwtTest.java --subject SomeUser \
            --issuer=MyAwesomeApp \
            --audience=AnotherApp \
            --audience=YetAnotherApp \
            --claim role=ADMIN \
            --claim org=MyOrg \
            --int-claim age=42 \
            --ttl 60
```

This will issue a JWT with the following payload (the `exp` and `iat` fields will be different):

```json
{
  "sub": "SomeUser",
  "aud": [
    "AnotherApp",
    "YetAnotherApp"
  ],
  "role": "ADMIN",
  "org": "MyOrg",
  "iss": "MyAwesomeApp",
  "exp": 1687433993,
  "iat": 1687433933,
  "age": 42
}
```

For a full list of options, run:

```bash
./JwtTest.java --help
```
