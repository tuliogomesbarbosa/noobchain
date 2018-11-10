package com.blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Block {
    public String hash;
    public String previousHash;
    public String merkleRoot;
    public List<Transaction> transactions = new ArrayList<>();
    private long timeStamp;
    private int nonce;

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return StringUtil.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot
        );
    }

    public void mineBlock(int difficulty) {
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block mined! : " + hash);
    }

    /**
     * Add transactions to this block
     * @param transaction
     * @return
     */
    public boolean addTransaction(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        if (previousHash != "0") {
            if (!transaction.processTransaction()) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction successfully added to this block");
        return true;
    }


}
