#Readme for Multi-Paxos based Logging Project
##Quick Start

Manual mode: Please run our code in IntelliJ IDEA IDE  
You can launch replica server and client by following the examples below:
    
    launch server: run ManualServiceLauncher0.java
                   run ManualServiceLauncher1.java
                   run ManualServiceLauncher2.java
                   run ManualServiceLauncher3.java
                   run ManualServiceLauncher4.java
      
    launch client: run ClientLauncher0.java
                   run ClientLauncher1.java
                   run ClientLauncher2.java
                   
    output: Each server writes the message in their log named 'replica{serverId}.log' under the root directory

Script Mode:
    
    launch server: 
        In terminal under the root directory of project, simply run 'java -jar out/artifacts/MultiPaxosLog_jar/MultiPaxosLog.jar {serverID}'
        
        For example: java -jar out/artifacts/MultiPaxosLog_jar/MultiPaxosLog.jar 0
        
        Note that you need to open serveral terminals and type the command above accordingly to launch several replica servers
        
        Configuration can be changed in src/service/script/config.properties
     
     launch client: (in IntelliJ IDEA)
        run ClientLauncher0.java
        run ClientLauncher1.java
        run ClientLauncher2.java
        
    output: Each server writes the message in their log named 'replica{serverId}.log' under the root directory
     
    
    
                   
    
    

