package be.scc.common;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate; // Solves a java version incompatibility error
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

public class SccEncryption {

    static public KeyPair GenerateKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }

    static public String serializeKey(Key key) {
        if (key instanceof RSAPublicKey)
            return serializeKey((RSAPublicKey) key);
        return serializeKey((RSAPrivateKey) key);
    }

    static private String serializeKey(RSAPublicKey pk) {
        return pk.getModulus().toString() + "|" + pk.getPublicExponent().toString();
    }

    static private String serializeKey(RSAPrivateKey pk) {
        return pk.getModulus().toString() + "|" + pk.getPrivateExponent().toString();
    }

    static public RSAPublicKey deserialisePublicKey(String key) throws GeneralSecurityException {
        String[] parts = key.split("\\|");
        RSAPublicKeySpec Spec = new RSAPublicKeySpec(
                new BigInteger(parts[0]),
                new BigInteger(parts[1]));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(Spec);
    }

    static public RSAPrivateKey deserialisePrivateKey(String key) throws GeneralSecurityException {
        String[] parts = key.split("\\|");
        RSAPrivateKeySpec Spec = new RSAPrivateKeySpec(
                new BigInteger(parts[0]),
                new BigInteger(parts[1]));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(Spec);
    }

    static public byte[] Encript(PublicKey publicKey, String plaintext)
            throws GeneralSecurityException {

        Cipher enc = Cipher.getInstance("RSA");

        enc.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = enc.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return cipherText;
    }

    static public String Decript(PrivateKey privateKey, byte[] cipherText) throws GeneralSecurityException {

        Cipher dec = Cipher.getInstance("RSA");

        dec.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] returned = dec.doFinal(cipherText);
        String plainText = new String(returned, StandardCharsets.UTF_8);
        return plainText;
    }
}
