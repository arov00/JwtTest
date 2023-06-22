///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS io.javalin:javalin:5.6.0
//DEPS org.slf4j:slf4j-simple:2.0.7
//DEPS com.auth0:java-jwt:4.3.0
//FILES simplelogger.properties
//JAVA 11+


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import io.javalin.Javalin;
import io.javalin.http.Context;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Command(name = "JwtTest", mixinStandardHelpOptions = true, version = "JwtTest 0.1",
        description = "JwtTest made with jbang")
class JwtTest implements Callable<Integer> {

    @Option(names = {"--jwks"}, description = "Path on which to serve the JWKS.", defaultValue = "/jwks", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String jwksPath;

    @Option(names = {"--port"}, description = "Port on which to serve the JWKS.", defaultValue = "7000", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private int port;

    @Option(names = {"--ttl"}, description = "Time in seconds until the issued JWT expires.", defaultValue = "3600", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private int ttl;

    @Option(names = {"--iss", "--issuer"} , description = "Issuer of the issued JWT.", defaultValue = "JwtTest", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String issuer;

    @Option(names = {"--sub", "--subject"} , description = "Subject of the issued JWT.", defaultValue = "JwtTest", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String subject;

    @Option(names = {"--aud", "--audience"} , description = "Audiences of the issued JWT.", defaultValue = "JwtTest", showDefaultValue = CommandLine.Help.Visibility.ALWAYS, arity = "1..*")
    private String[] audience;

    @Option(names = {"-c", "--claim"}, description = "String-typed claims to add to the issued JWT.", arity = "0..*", paramLabel = "<key=value>")
    private Map<String, String> claims;

    @Option(names = {"--int-claim"}, description = "Integer-typed claims to add to the issued JWT.", arity = "0..*", paramLabel = "<key=value>")
    private Map<String, Integer> intClaims;

    private KeyPair keyPair;

    private String keyId = "test";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtTest.class);

    public static void main(String... args) {
        // if the first arg is the command name, so skip it
        if (args.length > 1 && !args[0].startsWith("-")) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR");
        new CommandLine(new JwtTest()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Javalin.create()
                .get(jwksPath, this::returnJwks)
                .start(port);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        printJwt();
        return 0;
    }

    private void returnJwks(Context ctx) {
        var pk = (RSAPublicKey)keyPair.getPublic();
        var exp = pk.getPublicExponent();
        var mod = pk.getModulus();
        var expBase64 = Base64.getUrlEncoder().encodeToString(exp.toByteArray());
        var modBase64 = Base64.getUrlEncoder().encodeToString(mod.toByteArray());
        String jwk = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"" + expBase64 + "\",\"use\":\"sig\",\"kid\":\"" + keyId + "\",\"n\":\"" + modBase64 + "\"}]}";
        ctx.contentType("application/json");
        ctx.result(jwk);
    }

    private void printJwt() {
        try {
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey)keyPair.getPublic(), (RSAPrivateKey)keyPair.getPrivate());
            var builder = JWT.create();
            if (claims != null)
                claims.forEach(builder::withClaim);
            if (intClaims != null)
                intClaims.forEach(builder::withClaim);
            builder
                .withIssuer(issuer)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(ttl))
                .withSubject(subject)
                .withAudience(audience)
                .withKeyId(keyId);
            log.info("Here's your JWT: \n\n{}\n", builder.sign(algorithm));
            log.info("To verify this JWT, fetch the JWKS from http://localhost:{}{}", port, jwksPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
