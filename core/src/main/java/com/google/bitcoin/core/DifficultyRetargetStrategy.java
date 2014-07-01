package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStoreException;

public interface DifficultyRetargetStrategy {

    void checkDifficultyTransition(StoredBlock storedPrev, Block nextBlock) throws BlockStoreException;
}
