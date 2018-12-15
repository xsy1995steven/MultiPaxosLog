package service.manual;

import service.PaxosLogServer;
import util.AddressPortPair;

import java.util.ArrayList;
import java.util.List;

/**
 * A server launcher that can be used to fly a replica server manually, feel free to change the parameters
 * as you need in different environments.
 * Note that if the skipSlotSeqNumber == -1, it meas disable the skip slot, you can change it to whatever you
 * want for your skip slot in this replica. For the messageLossRate, 0.0 means that there is no message been
 * dropped, you are free to change it to a small value like 0.1, our system can guarantee correct functionality.
 *
 * Each replica server is a process, you should run them separately like run ManualServiceLauncher0, ManualServiceLauncher1
 * ManualServiceLauncher2...
 */
public class ManualServiceLauncher0 {

    public static void main(String[] args) {
        final List<AddressPortPair> allReplicasInfo = new ArrayList<>();
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3057));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3058));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3059));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3060));
        allReplicasInfo.add(new AddressPortPair("127.0.0.1", 3061));

        final PaxosLogServer logServer = new PaxosLogServer(
                0,
                "127.0.0.1",
                3057,
                true,
                0,
                2,
                allReplicasInfo,
                -1,
                0.1
        );

        logServer.start();
    }

}
