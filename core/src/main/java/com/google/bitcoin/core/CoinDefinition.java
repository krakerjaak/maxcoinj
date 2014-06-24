package com.google.bitcoin.core;

import static com.google.bitcoin.core.Utils.doubleDigest;
import static com.google.bitcoin.core.Utils.reverseBytes;

/**
 * Represents a globally-replaceable strategy for the coin. This class has been created to facilitate porting
 * of the BitcoinJ code to other coins.
 * To support another coin, call CoinDefinition.setGlobalInstance() just before initializing anything.
 */
public class CoinDefinition {

    public byte[] addressChecksum(byte[] bytes) {
        return addressChecksum(bytes, 0, bytes.length);
    }

    public byte[] addressChecksum(byte[] bytes, int offset, int length) {
        return Utils.doubleDigest(bytes, offset, length);
    }

    public byte[] transactionHash(byte[] bits) {
        return reverseBytes(doubleDigest(bits));
    }

    public byte[] blockHash(byte[] bytes) {
        return Utils.reverseBytes(doubleDigest(bytes));
    }

    private static CoinDefinition globalInstance = new CoinDefinition();
    public static void setGlobalInstance(CoinDefinition definition) {
        globalInstance = definition;
    }

    public static CoinDefinition get() {
        return globalInstance;
    }
}
