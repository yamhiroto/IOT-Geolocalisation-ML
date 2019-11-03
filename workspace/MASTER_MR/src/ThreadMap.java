
public class ThreadMap implements Runnable {

    private String _machineName;
    private String _splitName;

    @Override
    public void run() {
        process();
    }

    private void process() {

        ProcessBuilder processBuilder2 = new ProcessBuilder(MASTER_MR.SSH_COMMAND, _machineName, "java", "-jar",
                MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + "/" + MASTER_MR.SLAVE_FILENAME,
                "0", MASTER_MR.FOLDER_NAME_TMP + MASTER_MR.FOLDER_NAME_PERSO + MASTER_MR.FOLDER_NAME_SPLITS + "/" + _splitName);
        MASTER_MR.startThreadProcessBuilder(processBuilder2);
        // @Todo: if jar executed without error, we display the following line
        System.out.println("map executed on machine " + _machineName);

    }

    public ThreadMap(String machineName, String splitName) {
        _machineName = machineName;
        _splitName = splitName;
    }
}
