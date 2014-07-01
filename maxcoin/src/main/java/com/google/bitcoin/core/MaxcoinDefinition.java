package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStore;
import fr.cryptohash.Keccak256;

import static com.google.bitcoin.core.Utils.reverseBytes;
import static com.google.bitcoin.core.Utils.singleDigest;

public class MaxcoinDefinition extends CoinDefinition {

    @Override
    public DifficultyRetargetStrategy getDifficultyRetargetStrategy(NetworkParameters params, BlockStore blockStore) {
        return new BlockHeightDifficultyRetargetStrategySelector(200,
                new BitcoinDifficultyRetargetStrategy(params, blockStore),
                new KimotoGravityWellDiffTargetStrategy(params, blockStore)
        );
    }

    @Override
    public byte[] addressChecksum(byte[] bytes, int offset, int length) {
        return hashKeccak(bytes, offset, length);
    }

    @Override
    public byte[] transactionHash(byte[] bytes) {
        return reverseBytes(singleDigest(bytes, 0, bytes.length));
    }

    @Override
    public byte[] blockHash(byte[] bytes, int offset, int length) {
        return reverseBytes(hashKeccak(bytes, offset, length));
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
