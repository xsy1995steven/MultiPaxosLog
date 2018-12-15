package util;

import service.PaxosLogServer;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


/**
 * A log entry slot manger that manage the slot entry for each replica
 */
public class LogEntrySlotManager {

    private static final int INITIAL_ARRAY_CAPACITY = 10;

    private final PaxosLogServer paxosLogServer;

    private final int skipSlotSeqNum;
    private final int serverId;

    // we use an array of LogEntry to model the entry slots, using array enable us to allow a hole (skip slot)
    private LogEntry[] logEntryArray;
    private final int size;

    private int firstUnchosenIndex; // the smallest log index that have not been chosen
    private int lastLogIndex;   // the largest entry for which this server has accepted a proposal
    private int minProposal;    // the number of the smallest proposal this server will accept for any log entry

    public LogEntrySlotManager(final PaxosLogServer paxosLogServer) {
        this.paxosLogServer = paxosLogServer;
        this.logEntryArray = new LogEntry[INITIAL_ARRAY_CAPACITY];
        this.size = 0;
        this.firstUnchosenIndex = 0;
        this.lastLogIndex = 0;
        this.minProposal = 0;
        this.skipSlotSeqNum = paxosLogServer.getSkipSlotSeqNum();
        this.serverId = paxosLogServer.getServerId();
    }

    public int getProposalID(final int index) {
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        final LogEntry logEntry = logEntryArray[index];
        if (logEntry == null) {
            return 0;
        } else {
            return logEntry.getAcceptedProposal();
        }
    }

    public String getLogEntryValue(final int index) {
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        final LogEntry logEntry = logEntryArray[index];
        if (logEntry == null) {
            return null;
        } else {
            return logEntry.getAcceptedValue();
        }
    }

    public void insertLogEntry(final int index, final int proposalID, final String value) {
        selfUpdate();
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        logEntryArray[index] = new LogEntry(proposalID, value);
        selfUpdate();
    }

    public void chooseLogEntry(final int index) {
        selfUpdate();
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        final LogEntry logEntry = logEntryArray[index];
        logEntry.setAcceptedProposal(Integer.MAX_VALUE);
        selfUpdate();
    }

    public void successLogEntry(final int index, final String value) {
        selfUpdate();
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        logEntryArray[index] = new LogEntry(Integer.MAX_VALUE, value);
        selfUpdate();
    }

    public boolean isEntryChosen(final int index) {
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        final LogEntry logEntry = logEntryArray[index];
        if (logEntry == null) {
            return false;
        } else {
            return logEntry.getAcceptedProposal() == Integer.MAX_VALUE;
        }
    }

    public void updateLogEntry(final int index, final int proposalID, final String value) {
        selfUpdate();
        if (index >= logEntryArray.length) {
            logEntryArrayExpand();
        }
        final LogEntry logEntry = logEntryArray[index];
        logEntry.setAcceptedProposal(proposalID);
        logEntry.setAcceptedValue(value);
        selfUpdate();
    }

    private void logEntryArrayExpand() {
        selfUpdate();
        final int previousSize = this.logEntryArray.length;
        LogEntry[] newLogEntryArray = new LogEntry[previousSize * 2];
        for (int i = 0; i < previousSize; i++) {
            newLogEntryArray[i] = logEntryArray[i];
        }
        this.logEntryArray = newLogEntryArray;
        selfUpdate();
    }

    private void selfUpdate() {
        updateFirstUnchosenIndex();
        updateLastLogIndex();
    }

    private void updateFirstUnchosenIndex() {
        for (int i = 0; i < logEntryArray.length; i++) {
            if (paxosLogServer.isLeader() && i == skipSlotSeqNum) {
                continue;
            }
            if (logEntryArray[i] == null) {
                this.firstUnchosenIndex = i;
                break;
            }
            if (logEntryArray[i].getAcceptedProposal() != Integer.MAX_VALUE) {
                this.firstUnchosenIndex = i;
                break;
            }
        }
    }

    private void updateLastLogIndex() {
        for (int i = logEntryArray.length - 1; i >= 0; i--) {
            if (logEntryArray[i] != null) {
                lastLogIndex = i;
                break;
            }
        }
    }

    public int getFirstUnchosenIndex() {
        selfUpdate();
        return firstUnchosenIndex;
    }

    public void setFirstUnchosenIndex(int firstUnchosenIndex) {
        selfUpdate();
        this.firstUnchosenIndex = firstUnchosenIndex;
    }

    public int getLastLogIndex() {
        selfUpdate();
        return lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        selfUpdate();
        this.lastLogIndex = lastLogIndex;
    }

    public int getMinProposal() {
        selfUpdate();
        return minProposal;
    }

    public void setMinProposal(int minProposal) {
        selfUpdate();
        this.minProposal = minProposal;
    }

    /**
     * Write the chosen value to the log of the replica
     * Note that you can write (execute) if and only if there is no holes in front of current slot
     */
    public void write() {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream("replica" + serverId + ".log", true)));
            for (LogEntry logEntry : logEntryArray) {
                if (logEntry == null) {
                    break;
                }
                if (logEntry.isExecuted()) {
                    continue;
                } else if (!logEntry.isExecuted() && logEntry.getAcceptedProposal() == Integer.MAX_VALUE) {
                    bufferedWriter.write(logEntry.getAcceptedValue());
                    bufferedWriter.newLine();
                    logEntry.setExecuted(true);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

