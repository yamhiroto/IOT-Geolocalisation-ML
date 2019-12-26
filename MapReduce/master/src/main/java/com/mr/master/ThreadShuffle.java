package com.mr.master;

import java.io.File;

public class ThreadShuffle implements Runnable {

    private String _machineName;

    @Override
    public void run() {
        try {
            process();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void process() throws InterruptedException {

        // *** Copy machines.txt file ***
        while (!Master.folderExist(_machineName, Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO)) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(Master.SSH_COMMAND, _machineName, Master.FOLDER_CREATION_COMMAND, Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO);
            System.out.println("Folder " + Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + " created on machine " + _machineName);
            Master.startThreadProcessBuilder(processBuilder2);
        }

        File f = new File(Master.FOLDER_RESOURCES + "/" + Master.MACHINES_FILENAME);
        if(f.exists()) {
            ProcessBuilder processBuilder3 = new ProcessBuilder(Master.COPY_COMMAND, Master.FOLDER_RESOURCES + "/" + Master.MACHINES_FILENAME,
                    _machineName + ":" + Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + "/");
            System.out.println("Copying " + Master.MACHINES_FILENAME + " on " + _machineName);
            Master.startThreadProcessBuilder(processBuilder3);
        } else {
            System.out.println(Master.MACHINES_FILENAME + " not found!!");
        }


        // *** Execute SLAVE for each map file ***
        /***
         * 1. Get all files in UM folder
         * 2. Loop on each file and execute the jar with the path file as parameter
         */

        ProcessBuilder processBuilder = new ProcessBuilder(Master.SSH_COMMAND, _machineName, Master.LS_COMMAND,
                Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + Master.FOLDER_NAME_MAPS);
        ThreadProcessBuilder myThread = Master.startThreadProcessBuilder(processBuilder);
        try {
            System.out.println("Getting all UM files on " + _machineName + "...");
            Thread.sleep(5000); // let some time so that the queue gets filled
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (String mapFileName : myThread.getQueue()) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(Master.SSH_COMMAND, _machineName, "java", "-jar",
                    Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + "/" + Master.SLAVE_FILENAME,
                    "1", Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + Master.FOLDER_NAME_MAPS + "/" + mapFileName);
            System.out.println("Executing jar on " + _machineName);
            Master.startThreadProcessBuilder(processBuilder2);
        }

    }

    public ThreadShuffle(String machineName) {
        _machineName = machineName;
    }
}
