package com.google.bitcoin.core;

import fr.cryptohash.Keccak256;

import static com.google.bitcoin.core.Utils.reverseBytes;
import static com.google.bitcoin.core.Utils.singleDigest;

public class MaxcoinDefinition extends CoinDefinition {

    @Override
    public byte[] addressChecksum(byte[] bytes, int offset, int length) {
        // single round of keccak256
        Keccak256 digest = new Keccak256();
        digest.digest(bytes, offset, length);
        return digest.digest();
    }

    @Override
    public byte[] transactionHash(byte[] bytes) {
        return reverseBytes(singleDigest(bytes, 0, bytes.length));
    }

//    public byte[] blockHash(byte[] bytes) {
//        return transactionHash(bytes);
//    }
}
