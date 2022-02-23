package com.util.updater;

import java.io.File;

public class Updater {

    private final String latest;
    private final CommandExecuter cmdExec= new CommandExecuter();

    public Updater(String latest){
        this.latest = latest;
    }

    public String windowsUpdater(){
        String res = "";
        String baseCmd = "cmd.exe /c cd " +  System.getProperty("user.dir") + " & taskkill /F /PID " + getPID() 
            + " & curl -L -O " + "https://github.com/DavidArthurCole/bhb/releases/download/" + latest;
        
        //If the exe exists, replace it
        if(new File(System.getProperty("user.dir") + "/BHB.exe").exists()){
            res = cmdExec.executeCommandWindows(baseCmd + "/BHB.exe & BHB.exe");
        }
        
        //If the jar exists, replace it
        if(new File(System.getProperty("user.dir") + "/BHB.jar").exists()){
            res = cmdExec.executeCommandWindows(baseCmd + "/BHB.jar & java -jar BHB.jar");
        }

        return res;
    }

    public String linuxUpdater(){
        return (cmdExec.executeCommandLinux("#!/bin/bash\n\ncd " + System.getProperty("user.dir") + 
            "\nwget https://github.com/DavidArthurCole/bhb/releases/download/" + 
            latest + "/BHB.jar -O BHB.jar && java -jar BHB.jar " + Long.toString(getPID())));
    }
    
    public String macOSUpdater(){
        return cmdExec.executeCommandMacOS("cd " + System.getProperty("user.dir") + 
            " && curl -H \"Accept: application/zip\" -L -o BHB.jar 'https://github.com/DavidArthurCole/bhb/releases/download/" + 
            latest + "/BHB.jar' && java -jar BHB.jar " + Long.toString(getPID()));
    }

    //Returns the working PID of the JVM on all OS-es
    private static long getPID() {
        return Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }
}