package client;


import util.AddressPortPair;

import java.util.ArrayList;
import java.util.List;

public class ClientLauncher2 {

    public static void main(String args[]) {

        final List<AddressPortPair> allReplicasInfo = new ArrayList<>();
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3057));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3058));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3059));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3060));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3061));

        final PaxosLogClient logClient = new PaxosLogClient(
                "127.0.0.1",
                9999,
                allReplicasInfo,
                0.0);

        logClient.start();
    }

}
