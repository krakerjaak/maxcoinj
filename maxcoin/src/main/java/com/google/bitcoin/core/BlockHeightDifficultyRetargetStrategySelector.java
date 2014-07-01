package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStoreException;

/**
 * Facade to select an actual difficulty retargeting strategy depending on block height.
 */
public class BlockHeightDifficultyRetargetStrategySelector implements DifficultyRetargetStrategy {

    private int blockHeightToSwitch;
    private DifficultyRetargetStrategy strategyBefore;
    private DifficultyRetargetStrategy strategyAfter;

    public BlockHeightDifficultyRetargetStrategySelector(int blockHeightToSwitch,
                                                         DifficultyRetargetStrategy strategyBefore,
                                                         DifficultyRetargetStrategy strategyAfter) {
        this.blockHeightToSwitch = blockHeightToSwitch;
        this.strategyBefore = strategyBefore;
        this.strategyAfter = strategyAfter;
    }

    @Override
    public void checkDifficultyTransition(StoredBlock storedPrev, Block nextBlock) throws BlockStoreException {
        if (storedPrev.getHeight()+1 < blockHeightToSwitch) {
            strategyBefore.checkDifficultyTransition(storedPrev, nextBlock);
        } else {
            strategyAfter.checkDifficultyTransition(storedPrev, nextBlock);
        }
    }
}
