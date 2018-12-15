package service.script;

import service.PaxosLogServer;
import util.AddressPortPair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class ScriptServiceLauncher {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Invalid arguments for Paxos replica server");
            System.exit(1);
        }

        final int serverId = Integer.parseInt(args[0]);

        final Properties properties = new Properties();
        try {
            InputStream in = ScriptServiceLauncher.class.getClassLoader().getResourceAsStream("service/script/config.properties");
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final List<AddressPortPair> allReplicasInfo = new ArrayList<>();
        final List<String> replicasIpAndPort = Arrays.asList(properties.getProperty("all_replicas_info").split(","));
        for (String info : replicasIpAndPort) {
            allReplicasInfo.add(new AddressPortPair(info.split(":")[0], Integer.parseInt(info.split(":")[1])));
        }

        final PaxosLogServer logServer = new PaxosLogServer(
                Integer.parseInt(properties.getProperty("server_id_" + serverId)),
                properties.getProperty("server_address_" + serverId),
                Integer.parseInt(properties.getProperty("sever_port_" + serverId)),
                Boolean.valueOf(properties.getProperty("sever_isLeader_" + serverId)),
                Integer.parseInt(properties.getProperty("view_number_" + serverId)),
                Integer.parseInt(properties.getProperty("num_of_tolerated_failures_" + serverId)),
                allReplicasInfo,
                Integer.parseInt(properties.getProperty("skip_slot_seq_number_" + serverId)),
                Double.parseDouble(properties.getProperty("message_loss_rate_" + serverId))
        );
        logServer.start();
    }
}

