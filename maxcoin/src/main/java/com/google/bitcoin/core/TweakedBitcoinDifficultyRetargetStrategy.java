package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStore;

/**
 * Maxcoin-specific tweak to the traditional Bitcoin difficulty re-targeting algorithm.
 * Other Maxcoin-specific parameters are used via the MaxcoinMainNetParams implementation.
 *
 * @see com.google.bitcoin.params.MaxcoinMainNetParams
 */
public class TweakedBitcoinDifficultyRetargetStrategy extends BitcoinDifficultyRetargetStrategy {

    public TweakedBitcoinDifficultyRetargetStrategy(NetworkParameters params, BlockStore blockStore) {
        super(params, blockStore);
    }

    @Override
    protected int calculateBlocksLookback(StoredBlock storedPrev) {
        // Franko: This fixes an issue where a 51% attack can change difficulty at will.
        // Go back the full period unless it's the first retarget after genesis. Code courtesy of Art Forz
        int blocksToGoBack = params.getInterval() - 1;
        if (storedPrev.getHeight()+1 != params.getInterval()) {
            blocksToGoBack = params.getInterval();
        }
        return blocksToGoBack;
    }
}
