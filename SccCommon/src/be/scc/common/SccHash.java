package be.scc.common;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SccHash {
    public final byte[] bytes;

    public SccHash(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return Util.base64(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SccHash sccHash = (SccHash) o;
        return Arrays.equals(bytes, sccHash.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
