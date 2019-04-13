package be.scc.common;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

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

        var priv = (RSAPrivateKey) pairOrig.getPrivate();
        var publ = (RSAPublicKey) pairOrig.getPublic();

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
        SecretKey key = SccEncryption.GenerateSymetricKey();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.Encript(key, origMessage);
        String resultText = SccEncryption.Decript(key, cypherText);
        assert (origMessage.equals(resultText));

        var serialised = SccEncryption.serializeKey(key);
        var deserialised = SccEncryption.deserialiseSymetricKey(serialised);
        assert deserialised.equals(key);
    }

    @Test
    void signature() throws Exception {
        var message = "Sign me plz.".repeat(1000);

        KeyPair pair = SccEncryption.GenerateKeypair();

        var sign = SccEncryption.Sign(pair.getPrivate(), message);

        assert SccEncryption.VerifySign(pair.getPublic(), message, sign);
        assert !SccEncryption.VerifySign(pair.getPublic(), "different message now" + message, sign);
    }
}
