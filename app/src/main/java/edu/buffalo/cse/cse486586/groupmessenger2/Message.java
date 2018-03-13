package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by shash on 3/11/2018.
 */

public class Message {
    private String sourcePort = "";
    private String sequenceOf = "";
    private String Message = "";
    private int sequenceNumber;
    private int finalSequenceNumber;
    private Enum<MessageStatus> status = MessageStatus.NEW;

    public Message() {
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getFinalSequenceNumber() {
        return finalSequenceNumber;
    }

    public void setFinalSequenceNumber(int finalSequenceNumber) {
        this.finalSequenceNumber = finalSequenceNumber;
    }

    public Enum<MessageStatus> getStatus() {
        return status;
    }

    public void setStatus(Enum<MessageStatus> status) {
        this.status = status;
    }

    public String getSequenceOf() {
        return sequenceOf;
    }

    public void setSequenceOf(String sequenceOf) {
        this.sequenceOf = sequenceOf;
    }

    @Override
    public String toString() {
        return Message + "@" + status + "@" + sourcePort + "@" + sequenceOf + "@" + finalSequenceNumber + "@" + sequenceNumber;
    }

    public static Message getMessageObject(String msg) {
        edu.buffalo.cse.cse486586.groupmessenger2.Message message = new Message();
        String[] input = msg.split("@");
        message.setMessage(input[0]);
        message.setSourcePort(input[2]);
        message.setSequenceOf(input[3]);
        message.setFinalSequenceNumber(Integer.valueOf(input[4]));
        message.setSequenceNumber(Integer.valueOf(input[5]));
        message.setStatus(MessageStatus.valueOf(input[1]));

        return message;
    }
}
