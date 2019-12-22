import java.io.File;

public class ThreadShuffle_sendTxtMachines implements Runnable {

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

        // *** Check whether private folder exists ***
        while (!MASTER_MR.folderExist(_machineName, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO)) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, MASTER_MR.FOLDER_CREATION_COMMAND, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO);
            System.out.println("Folder " + MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + " created on machine " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder2);
        }

        // *** Copy machines.txt file ***
        File f = new File(MASTER_MR.WORKSPACE_FOLDER + "/" + MASTER_MR.MACHINES_FILENAME);
        if(f.exists()) {
            ProcessBuilder processBuilder3 = new ProcessBuilder(MASTER_MR.COPY_COMMAND, MASTER_MR.WORKSPACE_FOLDER + "/" + MASTER_MR.MACHINES_FILENAME,
                    _machineName + ":" + MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + "/");
            System.out.println("Copying " + MASTER_MR.MACHINES_FILENAME + " on " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder3);
        } else {
            System.out.println(MASTER_MR.MACHINES_FILENAME + " not found!!");
        }

    }

    public ThreadShuffle_sendTxtMachines(String machineName) {
        _machineName = machineName;
    }
}
