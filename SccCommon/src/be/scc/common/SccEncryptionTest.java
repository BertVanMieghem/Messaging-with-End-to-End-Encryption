package be.scc.common;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class SccEncryptionTest {

    @Test
    void makeKeyEncryptDecrypt() throws Exception {
        KeyPair pair = SccEncryption.generateKeypair();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.encrypt(pair.getPublic(), origMessage);
        String resultText = SccEncryption.decrypt(pair.getPrivate(), cypherText);
        assert (origMessage.equals(resultText));
    }

    @Test
    void serialising() throws Exception {
        KeyPair pairOrig = SccEncryption.generateKeypair();

        var priv = (RSAPrivateKey) pairOrig.getPrivate();
        var publ = (RSAPublicKey) pairOrig.getPublic();

        var privStr = SccEncryption.serializeKey(priv);
        var publStr = SccEncryption.serializeKey(publ);

        KeyPair pair = new KeyPair(SccEncryption.deserialisePublicKey(publStr), SccEncryption.deserialisePrivateKey(privStr));

        {
            String origMessage = "Hello test!";
            byte[] cypherText = SccEncryption.encrypt(pair.getPublic(), origMessage);
            String resultText = SccEncryption.decrypt(pairOrig.getPrivate(), cypherText);
            assert (origMessage.equals(resultText));
        }

        {
            String origMessage = "Hello test!";
            byte[] cypherText = SccEncryption.encrypt(pair.getPublic(), origMessage);
            String resultText = SccEncryption.decrypt(pair.getPrivate(), cypherText);
            assert (origMessage.equals(resultText));
        }
    }

    @Test
    void symetric() throws Exception {
        SecretKey key = SccEncryption.generateSymetricKey();

        String origMessage = "Hello test!";
        byte[] cypherText = SccEncryption.encrypt(key, origMessage);
        String resultText = SccEncryption.decrypt(key, cypherText);
        assert (origMessage.equals(resultText));

        var serialised = SccEncryption.serializeKey(key);
        var deserialised = SccEncryption.deserialiseSymetricKey(serialised);
        assert deserialised.equals(key);
    }

    @Test
    void signature() throws Exception {
        var message = "sign me plz.".repeat(1000);

        KeyPair pair = SccEncryption.generateKeypair();

        var sign = SccEncryption.sign(pair.getPrivate(), message);

        assert SccEncryption.verifySign(pair.getPublic(), message, sign);
        assert !SccEncryption.verifySign(pair.getPublic(), "different message now" + message, sign);
    }

    @Test
    void hash() throws Exception {

        var message1 = "Hash me.".repeat(1000);
        var message3 = "Jow.";

        // Warmup
        SccEncryption.SlowHash(message1.getBytes());

        long start = System.nanoTime();

        SccHash hash1 = SccEncryption.Hash(message1.getBytes());
        SccHash hash2 = SccEncryption.Hash(message1.getBytes()); // Doubled
        SccHash hash3 = SccEncryption.Hash(message3.getBytes());

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("timeElapsed: " + (timeElapsed / 1000000));
        System.out.println(hash1);
        System.out.println(hash3);

        assert hash1.equals(hash2);
        assert !hash1.equals(hash3);
    }

    @Test
    void hashSlow() throws Exception {
        var message1 = "Hash me.".repeat(1000);
        var message3 = "Jow.";

        // Warmup
        SccEncryption.SlowHash(message1.getBytes());

        long start = System.nanoTime();

        SccHash hash1 = SccEncryption.SlowHash(message1.getBytes());
        SccHash hash2 = SccEncryption.SlowHash(message1.getBytes()); // Doubled
        SccHash hash3 = SccEncryption.SlowHash(message3.getBytes());

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("timeElapsed: " + (timeElapsed / 1000000));
        System.out.println(hash1);
        System.out.println(hash3);

        assert hash1.equals(hash2);
        assert !hash1.equals(hash3);
    }

    /*
    public byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        return bytes;
    }

    @Test
    void makeSalt() {
        var s = generateSalt();
        System.out.println(Util.base64(s));
    }*/
}
