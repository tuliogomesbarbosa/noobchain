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
