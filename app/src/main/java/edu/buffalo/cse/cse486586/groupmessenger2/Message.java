package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by shash on 3/11/2018.
 */

public class Message implements Serializable {
    private String sourcePort;
    private String Message;
    private int sequenceNumber = -1;
    private int finalSequenceNumber = -1;
    private int replyCount;
    private String fromPort;
    private Enum<MessageStatus> status;

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

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        this.fromPort = fromPort;
    }

    public Enum<MessageStatus> getStatus() {
        return status;
    }

    public void setStatus(Enum<MessageStatus> status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sourcePort='" + sourcePort + '\'' +
                ", Message='" + Message + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", finalSequenceNumber=" + finalSequenceNumber +
                ", replyCount=" + replyCount +
                ", fromPort='" + fromPort + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
