package com.mr.master;

public class ThreadMap implements Runnable {

    private String _machineName;
    private String _splitName;

    @Override
    public void run() {
        process();
    }

    private void process() {

        ProcessBuilder processBuilder = new ProcessBuilder(Master.SSH_COMMAND, _machineName, "java", "-jar",
                Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + "/" + Master.SLAVE_FILENAME,
                "0", Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + Master.FOLDER_NAME_SPLITS + "/" + _splitName);
        Master.startThreadProcessBuilder(processBuilder);
        // @Todo: handle error during jar execution
        System.out.println("map executed on machine " + _machineName);

    }

    public ThreadMap(String machineName, String splitName) {
        _machineName = machineName;
        _splitName = splitName;
    }
}
