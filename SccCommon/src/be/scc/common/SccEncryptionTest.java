package be.scc.common;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SccEncryptionTest {

    @Test
    void makeKeyEncryptDecrypt() throws Exception {
        KeyPair pair = SccEncryption.GenerateKeypair();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.Encript(pair.getPublic(), origMessage);
        String resultText = SccEncryption.Decript(pair.getPrivate(), cypherText);
        assert (origMessage.equals(resultText));
    }

    @Test
    void serialising() throws Exception {
        KeyPair pairOrig = SccEncryption.GenerateKeypair();

        PrivateKey priv = pairOrig.getPrivate();
        PublicKey publ = pairOrig.getPublic();

        var privStr = SccEncryption.serializeKey(priv);
        var publStr = SccEncryption.serializeKey(publ);

        KeyPair pair = new KeyPair(SccEncryption.deserialisePublicKey(publStr), SccEncryption.deserialisePrivateKey(privStr));

        {
            String origMessage = "Hello test!";
            byte[] cypherText = SccEncryption.Encript(pair.getPublic(), origMessage);
            String resultText = SccEncryption.Decript(pairOrig.getPrivate(), cypherText);
            assert (origMessage.equals(resultText));
        }

        {
            String origMessage = "Hello test!";
            byte[] cypherText = SccEncryption.Encript(pair.getPublic(), origMessage);
            String resultText = SccEncryption.Decript(pair.getPrivate(), cypherText);
            assert (origMessage.equals(resultText));
        }
    }

    @Test
    void symetric() throws Exception {
        var key = SccEncryption.GenerateSymetricKey();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.Encript(key, origMessage);
        String resultText = SccEncryption.Decript(key, cypherText);
        assert (origMessage.equals(resultText));
    }
}