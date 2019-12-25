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
        while (!MASTER_MR.folderExist(_machineName, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO)) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, MASTER_MR.FOLDER_CREATION_COMMAND, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO);
            System.out.println("Folder " + MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + " created on machine " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder2);
        }

        File f = new File(MASTER_MR.WORKSPACE_FOLDER + "/" + MASTER_MR.MACHINES_FILENAME);
        if(f.exists()) {
            ProcessBuilder processBuilder3 = new ProcessBuilder(MASTER_MR.COPY_COMMAND, MASTER_MR.WORKSPACE_FOLDER + "/" + MASTER_MR.MACHINES_FILENAME,
                    _machineName + ":" + MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + "/");
            System.out.println("Copying " + MASTER_MR.MACHINES_FILENAME + " on " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder3);
        } else {
            System.out.println(MASTER_MR.MACHINES_FILENAME + " not found!!");
        }


        // *** Execute SLAVE for each map file ***
        /***
         * 1. Get all files in UM folder
         * 2. Loop on each file and execute the jar with the path file as parameter
         */

        ProcessBuilder processBuilder = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, MASTER_MR.LS_COMMAND,
                MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + MASTER_MR.FOLDER_NAME_MAPS);
        ThreadProcessBuilder myThread = MASTER_MR.startThreadProcessBuilder(processBuilder);
        try {
            System.out.println("Getting all UM files on " + _machineName + "...");
            Thread.sleep(5000); // let some time so that the queue gets filled
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (String mapFileName : myThread.getQueue()) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, "java", "-jar",
                    MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + "/" + MASTER_MR.SLAVE_FILENAME,
                    "1", MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + MASTER_MR.FOLDER_NAME_MAPS + "/" + mapFileName);
            System.out.println("Executing jar on " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder2);
        }

    }

    public ThreadShuffle(String machineName) {
        _machineName = machineName;
    }
}
