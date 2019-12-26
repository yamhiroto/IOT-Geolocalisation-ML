package com.mr.master;

import java.nio.file.Path;
import java.nio.file.Paths;

/***
 * 1. Check whether the split folder exists; if not, create it
 * 2. Copy the split file to the distant machine
 */

public class ThreadDeploySplit implements Runnable {

    private String _machineName;
    private String _splitName;

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process() throws Exception {

        Path p = Paths.get(_splitName);
        if (p != null) {
            if (_machineName == null) {
                throw new Exception("No machine available!");
            }
            // *** Folder '/splits' creation ***
            while (!Master.folderExist(_machineName, Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + Master.FOLDER_NAME_SPLITS)) {
                ProcessBuilder processBuilder2 = new ProcessBuilder(Master.SSH_COMMAND, _machineName, Master.FOLDER_CREATION_COMMAND, Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + "/splits");
                System.out.println("Folder created on machine " + _machineName);
                Master.startThreadProcessBuilder(processBuilder2);
            }

            // *** Copy of split files ***
            ProcessBuilder processBuilder3 = new ProcessBuilder(Master.COPY_COMMAND, Master.FOLDER_RESOURCES + "/" + _splitName, _machineName + ":" + Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + Master.FOLDER_NAME_SPLITS + "/");
            System.out.println("Copying split file on " + _machineName);
            Master.startThreadProcessBuilder(processBuilder3);
        }
    }


    public ThreadDeploySplit(String machineName, String splitName) {
        _machineName = machineName;
        _splitName = splitName;
    }

}
