package com.mr.master;

public class ThreadReduce implements Runnable {

    private String _machineName;

    @Override
    public void run() {
        process();
    }

    private void process() {

        ProcessBuilder processBuilder = new ProcessBuilder(Master.SSH_COMMAND, _machineName, "java", "-jar",
                Master.FOLDER_NAME_TMP + Master.FOLDER_NAME_PERSO + "/" + Master.SLAVE_FILENAME, "2");
        ThreadProcessBuilder t = Master.startThreadProcessBuilder(processBuilder);
        while (!t.isProcessFinished()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // @Todo: handle error during jar execution
        System.out.println("reduce executed on machine " + _machineName);

    }

    public ThreadReduce(String machineName) {
        _machineName = machineName;
    }
}
