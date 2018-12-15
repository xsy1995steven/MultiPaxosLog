package client;


import util.AddressPortPair;

import java.util.ArrayList;
import java.util.List;

/**
 * A client can only be launched manually through this client launcher, you are free to modify the port that your client
 * are listening to. Note that each client is a process that should be launch separately.
 * You are free fly several clients and connect to the replicas as long as they are listening to different ports.
 */
public class ClientLauncher0 {

    public static void main(String args[]) {

        final List<AddressPortPair> allReplicasInfo = new ArrayList<>();
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3057));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3058));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3059));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3060));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3061));

        final PaxosLogClient logClient = new PaxosLogClient(
                "127.0.0.1",
                7777,
                allReplicasInfo,
                0.0);

        logClient.start();
    }

}
