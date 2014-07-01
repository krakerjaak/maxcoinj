package com.google.bitcoin.core;

import fr.cryptohash.Keccak256;

import static com.google.bitcoin.core.Utils.reverseBytes;
import static com.google.bitcoin.core.Utils.singleDigest;

public class MaxcoinDefinition extends CoinDefinition {

    @Override
    public byte[] addressChecksum(byte[] bytes, int offset, int length) {
        return hashKeccak(bytes, offset, length);
    }

    @Override
    public byte[] transactionHash(byte[] bytes) {
        return reverseBytes(singleDigest(bytes, 0, bytes.length));
    }

    @Override
    public byte[] blockHash(byte[] bytes) {
        return reverseBytes(hashKeccak(bytes, 0, bytes.length));
    }

    @Override
    public byte[] messageChecksum(byte[] payloadBytes) {
        return hashKeccak(payloadBytes, 0, payloadBytes.length);
    }

    private byte[] hashKeccak(byte[] bytes, int offset, int length) {
        // single round of keccak256
        Keccak256 digest = new Keccak256();
        digest.update(bytes, offset, length);
        return digest.digest();
    }
}
