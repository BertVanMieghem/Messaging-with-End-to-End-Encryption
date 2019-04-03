package be.scc.common;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class SccEncryptionTest {

    // Encryption
    @Test
    void makeKeyEncryptDecrypt() throws Exception {
        KeyPair pair = SccEncryption.GenerateKeypair();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.Encript(pair.getPublic(), origMessage);
        String resultText = SccEncryption.Decript(pair.getPrivate(), cypherText);
        assert (origMessage.equals(resultText));
    }
}