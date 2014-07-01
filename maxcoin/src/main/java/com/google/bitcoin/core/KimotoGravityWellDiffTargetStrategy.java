package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static java.lang.Math.pow;

public class KimotoGravityWellDiffTargetStrategy implements DifficultyRetargetStrategy {
    private static final Logger log = LoggerFactory.getLogger(BitcoinDifficultyRetargetStrategy.class);

    private static final int TIME_DAY_SECONDS = 60 * 60 * 24;
    private static final long PAST_SECONDS_MIN = TIME_DAY_SECONDS / 100;
    private static final long PAST_SECONDS_MAX = TIME_DAY_SECONDS * 14 / 100;

    private final NetworkParameters params;
    private final BlockStore blockStore;

    // calculated values
    private int blockTargetSpacing;
    private long pastBlocksMin;
    private long pastBlocksMax;

    public KimotoGravityWellDiffTargetStrategy(NetworkParameters params, BlockStore blockStore) {

        this.params = params;
        this.blockStore = blockStore;

        this.blockTargetSpacing = params.getBlockTargetSpacingSeconds();
        this.pastBlocksMin = PAST_SECONDS_MIN / blockTargetSpacing;
        this.pastBlocksMax = PAST_SECONDS_MAX / blockTargetSpacing;
    }

    @Override
    public void checkDifficultyTransition(StoredBlock storedPrev, Block nextBlock) throws BlockStoreException {

        kimotoGravityWell(storedPrev, nextBlock,
                blockTargetSpacing,
                pastBlocksMin,
                pastBlocksMax
        );
    }

    private void kimotoGravityWell(StoredBlock storedPrev, Block nextBlock, long targetBlocksSpacingSeconds, long pastBlocksMin, long pastBlocksMax) throws BlockStoreException, VerificationException {
        /* current difficulty formula, megacoin - kimoto gravity well */
        //const CBlockIndex  *BlockLastSolved				= pindexLast;
        //const CBlockIndex  *BlockReading				= pindexLast;
        //const CBlockHeader *BlockCreating				= pblock;
        StoredBlock blockLastSolved = storedPrev;
        StoredBlock blockReading = storedPrev;

        long pastBlocksMass = 0;
        long pastRateActualSeconds = 0;
        long pastRateTargetSeconds = 0;
        double pastRateAdjustmentRatio = 1f;
        BigInteger pastDifficultyAverage = BigInteger.valueOf(0);
        BigInteger pastDifficultyAveragePrev = BigInteger.valueOf(0);

        double eventHorizonDeviation,
                eventHorizonDeviationFast,
                eventHorizonDeviationSlow;

        if (blockLastSolved == null || blockLastSolved.getHeight() == 0 || (long) blockLastSolved.getHeight() < pastBlocksMin) {
            return;
        }

        long latestBlockTime = blockLastSolved.getHeader().getTimeSeconds();

        for (int i = 1; blockReading.getHeight() > 0; i++) {
            if (pastBlocksMax > 0 && i > pastBlocksMax) {
                break;
            }
            pastBlocksMass++;

            if (i == 1) {
                pastDifficultyAverage = blockReading.getHeader().getDifficultyTargetAsInteger();
            } else {
                pastDifficultyAverage = ((blockReading.getHeader().getDifficultyTargetAsInteger()
                        .subtract(pastDifficultyAveragePrev))
                        .divide(BigInteger.valueOf(i))
                        .add(pastDifficultyAveragePrev));
            }
            pastDifficultyAveragePrev = pastDifficultyAverage;

            // maxcoin main.pp:1243
            if (latestBlockTime < blockReading.getHeader().getTimeSeconds()) {
                if (blockReading.getHeight() > 177500) {
                    latestBlockTime = blockReading.getHeader().getTimeSeconds();
                }
            }

            pastRateActualSeconds = latestBlockTime - blockReading.getHeader().getTimeSeconds();
            pastRateTargetSeconds = targetBlocksSpacingSeconds * pastBlocksMass;
            pastRateAdjustmentRatio = 1.0d;

            // maxcoin main.pp:1253
            if (blockReading.getHeight() > 177500) {
                if (pastRateActualSeconds < 1) {
                    pastRateActualSeconds = 1;
                }
            } else {
                if (pastRateActualSeconds < 0) {
                    pastRateActualSeconds = 0;
                }
            }

            if (pastRateActualSeconds != 0 && pastRateTargetSeconds != 0) {
                pastRateAdjustmentRatio = (double) pastRateTargetSeconds / (double) pastRateActualSeconds;
            }
            eventHorizonDeviation = 1 + (0.7084 * pow(((double) pastBlocksMass / 28.2d), -1.228d));
            eventHorizonDeviationFast = eventHorizonDeviation;
            eventHorizonDeviationSlow = 1 / eventHorizonDeviation;

            if (pastBlocksMass >= pastBlocksMin) {
                if ((pastRateAdjustmentRatio <= eventHorizonDeviationSlow) || (pastRateAdjustmentRatio >= eventHorizonDeviationFast)) {
                    break;
                }
            }
            StoredBlock blockReadingPrev = blockStore.get(blockReading.getHeader().getPrevBlockHash());
            if (blockReadingPrev == null) {
                break;
            }
            blockReading = blockReadingPrev;
        }

        /*CBigNum bnNew(PastDifficultyAverage);
        if (PastRateActualSeconds != 0 && PastRateTargetSeconds != 0) {
            bnNew *= PastRateActualSeconds;
            bnNew /= PastRateTargetSeconds;
        } */

        BigInteger newDifficulty = pastDifficultyAverage;
        if (pastRateActualSeconds != 0 && pastRateTargetSeconds != 0) {
            newDifficulty = newDifficulty.multiply(BigInteger.valueOf(pastRateActualSeconds));
            newDifficulty = newDifficulty.divide(BigInteger.valueOf(pastRateTargetSeconds));
        }

        if (newDifficulty.compareTo(params.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newDifficulty.toString(16));
            newDifficulty = params.getMaxTarget();
        }

        verifyDifficulty(newDifficulty, nextBlock);
    }

    private void verifyDifficulty(BigInteger calcDiff, Block nextBlock) {
        if (calcDiff.compareTo(params.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", calcDiff.toString(16));
            calcDiff = params.getMaxTarget();
        }
        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        BigInteger receivedDifficulty = nextBlock.getDifficultyTargetAsInteger();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        calcDiff = calcDiff.and(mask);

        if (calcDiff.compareTo(receivedDifficulty) != 0)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    receivedDifficulty.toString(16) + " vs " + calcDiff.toString(16));
    }
}
