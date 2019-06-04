package be.scc.common;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;


public class SccEncryption {
    static private IvParameterSpec iv = new IvParameterSpec("jsldghdj;figshig".getBytes(StandardCharsets.UTF_8)); // semi random ;)

    /**
     * This takes
     * ~265ms for keysize 2048
     * ~35ms for keysize 1024.
     * ~7ms for keysize 512
     * Key length based on: https://www.keylength.com/en/compare/
     */
    public static KeyPair generateKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }


    public static String serializeKey(PublicKey key) {
        return serializeKey((RSAPublicKey) key);
    }

    public static String serializeKey(RSAPublicKey key) {
        if (key == null) return null;
        return key.getModulus().toString() + "|" + key.getPublicExponent().toString();
    }

    public static String serializeKey(PrivateKey key) {
        return serializeKey((RSAPrivateKey) key);
    }

    public static String serializeKey(RSAPrivateKey key) {
        if (key == null) return null;
        return key.getModulus().toString() + "|" + key.getPrivateExponent().toString();
    }

    public static String serializeKey(SecretKey key) {
        if (key == null) return null;
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    static public RSAPublicKey deserialisePublicKey(String key) throws GeneralSecurityException {
        String[] parts = key.split("\\|");
        RSAPublicKeySpec Spec = new RSAPublicKeySpec(
                new BigInteger(parts[0]),
                new BigInteger(parts[1]));
        return ((RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(Spec));
    }

    static public RSAPrivateKey deserialisePrivateKey(String key) throws GeneralSecurityException {
        String[] parts = key.split("\\|");
        RSAPrivateKeySpec Spec = new RSAPrivateKeySpec(
                new BigInteger(parts[0]),
                new BigInteger(parts[1]));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(Spec);
    }

    static public byte[] encrypt(PublicKey publicKey, String plaintext)
            throws GeneralSecurityException {

        Cipher enc = Cipher.getInstance("RSA");

        enc.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = enc.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return cipherText;
    }

    static public String decrypt(PrivateKey privateKey, byte[] cipherText) throws GeneralSecurityException {

        Cipher dec = Cipher.getInstance("RSA");

        dec.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] returned = dec.doFinal(cipherText);
        String plainText = new String(returned, StandardCharsets.UTF_8);
        return plainText;
    }

    static public SecretKeySpec deserialiseSymetricKey(String key) throws GeneralSecurityException {
        SecretKeySpec spec = new SecretKeySpec(
                Base64.getDecoder().decode(key),
                "AES");
        return spec;
        //return (SecretKey) KeyGenerator.getInstance("AES").(Spec);
    }

    static public SecretKeySpec generateSymetricKey() throws GeneralSecurityException {
        SecureRandom secureRandom = new SecureRandom();
        int keyBitSize = 256;
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keyBitSize, secureRandom);
        return (SecretKeySpec) keyGenerator.generateKey();
    }

    static public byte[] concatBytes(byte[] a, byte[] b) {
        byte[] destination = new byte[a.length + b.length];
        System.arraycopy(a, 0, destination, 0, a.length);
        System.arraycopy(b, 0, destination, a.length, b.length);
        return destination;
    }

    private static final int randomPrefixLength = 5;

    static public byte[] encrypt(SecretKey key, String plaintext) throws GeneralSecurityException {
        Cipher enc = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        enc.init(Cipher.ENCRYPT_MODE, key, iv);

        var rawText = plaintext.getBytes(StandardCharsets.UTF_8);

        SecureRandom secureRandom = new SecureRandom();
        var randomB = new byte[randomPrefixLength];
        secureRandom.nextBytes(randomB);

        return enc.doFinal(concatBytes(randomB, rawText));
    }

    static public String decrypt(SecretKey key, byte[] cipherText) throws GeneralSecurityException {
        Cipher dec = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        dec.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] returned = dec.doFinal(cipherText);
        returned = Arrays.copyOfRange(returned, randomPrefixLength, returned.length); // Strip off the random prefix
        return new String(returned, StandardCharsets.UTF_8);
    }

    static public byte[] sign(PrivateKey privateKey, String data) throws Exception {
        Signature rsasign = Signature.getInstance("SHA1withRSA");
        rsasign.initSign(privateKey);
        rsasign.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signature = rsasign.sign();
        return signature;
    }

    static public boolean verifySign(PublicKey publicKey, String data, byte[] signature) throws Exception {
        Signature rsacheck = Signature.getInstance("SHA1withRSA");
        rsacheck.initVerify(publicKey);
        rsacheck.update(data.getBytes(StandardCharsets.UTF_8));
        boolean result = rsacheck.verify(signature);
        return result;
    }

    static public SccHash hash(byte[] toBeHashed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        md.update(Util.base64("MuW1OvmzZ8E1wzMugR9p3evkcrw="));
        byte[] digest = md.digest(toBeHashed);
        return new SccHash(digest);
    }

    /**
     * Maybe it would be better to use bcrypt
     * For making it extra difficult to reverse the hash, a salt per user could be provided.
     * However, hash is atm only used to hash the userId and is not considered security critical
     */
    static public SccHash slowHash(byte[] toBeHashed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        md.update(Util.base64("MuW1OvmzZ8E1wzMugR9p3evkcrw="));

        for (int i = 0; i < 15000; i++) {
            toBeHashed = hash(toBeHashed).bytes;
        }

        return new SccHash(toBeHashed);
    }

}
