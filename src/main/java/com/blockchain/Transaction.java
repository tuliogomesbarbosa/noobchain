package com.blockchain;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String transactionId;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0; //a rough count of how many transactions have been generated.

    public Transaction(PublicKey sender, PublicKey recipient, float value, ArrayList<TransactionInput> inputs) {
        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
        this.inputs = inputs;
    }

    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(recipient) +
                Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public boolean processTransaction() {
        if(!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //gather transaction inputs (make sure they are unspent)
        inputs.forEach(t -> t.UTXO = NoobChain.UTXOs.get(t.transactionOutputId));

        float unspentTxOutputSum = getInputsValues();

        if (unspentTxOutputSum < NoobChain.minimumTransaction) {
            System.out.println("#Transaction inputs too small: " + unspentTxOutputSum);
            return false;
        }

        //generate transactions outputs
        float leftOver = unspentTxOutputSum - value;
        this.transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, this.transactionId));
        outputs.add(new TransactionOutput(this.sender, leftOver, this.transactionId));

        //add outputs to unspent list
        outputs.forEach(o -> NoobChain.UTXOs.put(o.id, o));

        //remove transaction inputs from UTXO list as spent
        inputs.stream()
                .filter(i -> i.UTXO != null)
                .forEach(i -> NoobChain.UTXOs.remove(i.UTXO.id));

        return true;
    }

    public float getInputsValues() {
        return (float) inputs.stream()
                .filter(i -> i.UTXO != null)
                .mapToDouble(i -> i.UTXO.value)
                .sum();
    }

    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput output : outputs) {
            total += output.value;
        }
        return total;
    }

    public static class TransactionInput {
        public String transactionOutputId; //Reference to TransactionOutputs -> transactionId
        public TransactionOutput UTXO; //Contains the Unspent transaction output

        public TransactionInput(String transactionOutputId) {
            this.transactionOutputId = transactionOutputId;
        }
    }

    public static class TransactionOutput {
        public String id;
        public PublicKey recipient; //also known as the new owner of these coins.
        public float value;
        public String parentTransactionId; //the id of the transaction this output was created in

        public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
            this.recipient = recipient;
            this.value = value;
            this.parentTransactionId = parentTransactionId;
            this.id = StringUtil.applySha256(StringUtil.getStringFromKey(recipient)+Float.toString(value)+parentTransactionId);
        }

        public boolean isMine(PublicKey publicKey) {
            return (publicKey == recipient);
        }
    }
}
