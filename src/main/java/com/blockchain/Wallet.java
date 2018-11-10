package com.blockchain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public Map<String, Transaction.TransactionOutput> UTXOs = new HashMap<>();


    public Wallet() {
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            keyPairGenerator.initialize(ecSpec, secureRandom);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns balance and store the UTXOs owned by this walled in {@link Wallet#UTXOs}
     * @return balance
     */
    public float getBalance() {
        NoobChain.UTXOs.forEach((id,utxo) -> {
            if (utxo.isMine(publicKey)) {
                this.UTXOs.put(utxo.id, utxo);
            }
        });

        return (float) this.UTXOs.entrySet()
                .stream()
                .mapToDouble(e -> e.getValue().value)
                .sum();
    }

    public Transaction sendFunds(PublicKey _recipient, float value) {
        if (getBalance() < value) {
            System.out.println("#Not enough funds to send transaction. Transaction discarded.");
            return null;
        }

        ArrayList<Transaction.TransactionInput> inputs = new ArrayList<>();

        float total = 0;
        for (Map.Entry<String, Transaction.TransactionOutput> entry : this.UTXOs.entrySet()) {
            Transaction.TransactionOutput utxo = entry.getValue();
            total += utxo.value;

            inputs.add(new Transaction.TransactionInput(utxo.id));
            if(total > value) break;
        }

        Transaction newTx = new Transaction(this.publicKey, _recipient, value, inputs);
        newTx.generateSignature(this.privateKey);

        inputs.forEach(i -> this.UTXOs.remove(i.transactionOutputId));
        return newTx;
    }
}
