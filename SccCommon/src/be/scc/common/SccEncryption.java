package be.scc.common;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate; // Solves a java version incompatibility error
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
