package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Date;

/**
 * This has been extracted from the original BitcoinJ code as-is.
 */
public class BitcoinDifficultyRetargetStrategy implements DifficultyRetargetStrategy {
    private static final Logger log = LoggerFactory.getLogger(BitcoinDifficultyRetargetStrategy.class);

    // February 16th 2012
    private static final Date testnetDiffDate = new Date(1329264000000L);

    protected NetworkParameters params;
    protected BlockStore blockStore;

    public BitcoinDifficultyRetargetStrategy(NetworkParameters params, BlockStore blockStore) {
        this.params = params;
        this.blockStore = blockStore;
    }

    @Override
    public void checkDifficultyTransition(StoredBlock storedPrev, Block nextBlock) throws BlockStoreException {
        Block prev = storedPrev.getHeader();

        // Is this supposed to be a difficulty transition point?
        if ((storedPrev.getHeight() + 1) % params.getInterval() != 0) {

            // TODO: Refactor this hack after 0.5 is released and we stop supporting deserialization compatibility.
            // This should be a method of the NetworkParameters, which should in turn be using singletons and a subclass
            // for each network type. Then each network can define its own difficulty transition rules.
            if (params.getId().equals(NetworkParameters.ID_TESTNET) && nextBlock.getTime().after(testnetDiffDate)) {
                checkTestnetDifficulty(storedPrev, prev, nextBlock);
                return;
            }

            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        long now = System.currentTimeMillis();
        StoredBlock cursor = blockStore.get(prev.getHash());

        int blocksLookback = calculateBlocksLookback(storedPrev);
        for (int i = 0; i < blocksLookback; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        long elapsed = System.currentTimeMillis() - now;
        if (elapsed > 50)
            log.info("Difficulty transition traversal took {}msec", elapsed);

        Block blockIntervalAgo = cursor.getHeader();
        int timespan = (int) (prev.getTimeSeconds() - blockIntervalAgo.getTimeSeconds());
        // Limit the adjustment step.
        final int targetTimespan = params.getTargetTimespan();
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4;
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4;

        BigInteger newTarget = Utils.decodeCompactBits(prev.getDifficultyTarget());
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan));

        if (newTarget.compareTo(params.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = params.getMaxTarget();
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    newTargetCompact + " vs " + receivedTargetCompact);
    }

    protected int calculateBlocksLookback(StoredBlock storedPrev) {
        return params.getInterval() - 1;
    }

    private void checkTestnetDifficulty(StoredBlock storedPrev, Block prev, Block next) throws VerificationException, BlockStoreException {

        // After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
        // and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
        // blocks are allowed if there has been a span of 20 minutes without one.
        final long timeDelta = next.getTimeSeconds() - prev.getTimeSeconds();
        // There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted when time
        // goes backwards.
        if (timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2) {
            // Walk backwards until we find a block that doesn't have the easiest proof of work, then check
            // that difficulty is equal to that one.
            StoredBlock cursor = storedPrev;
            while (!cursor.getHeader().equals(params.getGenesisBlock()) &&
                    cursor.getHeight() % params.getInterval() != 0 &&
                    cursor.getHeader().getDifficultyTargetAsInteger().equals(params.getMaxTarget()))
                cursor = cursor.getPrev(blockStore);
            BigInteger cursorTarget = cursor.getHeader().getDifficultyTargetAsInteger();
            BigInteger newTarget = next.getDifficultyTargetAsInteger();
            if (!cursorTarget.equals(newTarget))
                throw new VerificationException("Testnet block transition that is not allowed: " +
                        Long.toHexString(cursor.getHeader().getDifficultyTarget()) + " vs " +
                        Long.toHexString(next.getDifficultyTarget()));
        }
    }
}
