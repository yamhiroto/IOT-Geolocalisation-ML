import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

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
            while (!MASTER_MR.folderExist(_machineName, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + MASTER_MR.FOLDER_NAME_SPLITS)) {
                ProcessBuilder processBuilder2 = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, MASTER_MR.FOLDER_CREATION_COMMAND, MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + "/splits");
                System.out.println("Folder created on machine " + _machineName);
                MASTER_MR.startThreadProcessBuilder(processBuilder2);
            }

            // *** Copy of split files ***
            ProcessBuilder processBuilder3 = new ProcessBuilder(MASTER_MR.COPY_COMMAND, MASTER_MR.HOME_FOLDER + "/Documents/workspace/MASTER_MR/" + _splitName, _machineName + ":" + MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + MASTER_MR.FOLDER_NAME_SPLITS + "/");
            System.out.println("Copying split file on " + _machineName);
            MASTER_MR.startThreadProcessBuilder(processBuilder3);
        }
    }


    public ThreadDeploySplit(String machineName, String splitName) {
        _machineName = machineName;
        _splitName = splitName;
    }

}
