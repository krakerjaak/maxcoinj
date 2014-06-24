package com.google.bitcoin.params;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Utils;

import static com.google.common.base.Preconditions.checkState;

public class MaxcoinMainNetParams extends NetworkParameters {

    public MaxcoinMainNetParams() {
        super();

        // PUSH 0x04 bytes | ffff001d | PUSH 0x01 byte | 04 | PUSH 0x34 bytes | "Shape-shifting software defends against botnet hacks" (as hex)
        genesisBlockScriptInput = Utils.HEX.decode("04ffff001d01043453686170652d7368696674696e6720736f66747761726520646566656e647320616761696e737420626f746e6574206861636b73");
        genesisBlockScriptPublicKey = Utils.HEX.decode("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f");
        genesisBlockReward = Coin.valueOf(5, 0);
        genesisBlockVersion = 112;
        genesisBlockDiffTarget = /*0x1d00ffffL*/0;

        interval = NetworkParameters.INTERVAL;
        targetTimespan = NetworkParameters.TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1d00ffffL);
        dumpedPrivateKeyHeader = 128;
        addressHeader = 112;
        p2shHeader = 110;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 8668;
        packetMagic = 0xf9beb4d9L;

        genesisBlock = createGenesis(this);
        genesisBlock.setTime(1390822264L);
        genesisBlock.setNonce(11548217);

        id = NetworkParameters.ID_MAINNET;
        subsidyDecreaseBlockCount = 210000;   // TODO: CHANGE THAT
        spendableCoinbaseDepth = 1;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("0000002d0f86558a6e737a3a351043ee73906fe077692dfaa3c9328aaca21964"),
                genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
        checkpoints.put(     0, new Sha256Hash("0000002d0f86558a6e737a3a351043ee73906fe077692dfaa3c9328aaca21964"));
        checkpoints.put( 25000, new Sha256Hash("000000000000892fe49518331d3ce99075a61ae03fe0c3fb5363babf793f9ed5"));
        checkpoints.put(111111, new Sha256Hash("0000000000023b44c09a7f8740cec05de8d88e7cbc606457cf86c45a8f1c2c1d"));

        dnsSeeds = new String[] {
                "maxcoin.cloudapp.net",
                "maxcoinus.cloudapp.net",
                "maxcoinasia.cloudapp.net",
                "maxexplorer.cloudapp.net"
        };
    }

    private static MaxcoinMainNetParams instance;
    public static synchronized MaxcoinMainNetParams get() {
        if (instance == null) {
            instance = new MaxcoinMainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return NetworkParameters.PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
