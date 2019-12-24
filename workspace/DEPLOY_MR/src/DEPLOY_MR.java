import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/***
 * @author gsavoure
 *
 * Question 2. STEP 7
 *
 */

public class DEPLOY_MR {

    public static final String FOLDER_PATH = "/tmp/";
    public static final String FOLDER_NAME = "savoga";
    public static final String SSH_COMMAND = "ssh";
    public static final String FOLDER_CREATION_COMMAND = "mkdir -p";
    public static final String LS_COMMAND = "ls";
    public static final String HOSTNAME_COMMAND = "hostname";
    public static final String USER_PREFIX = "gsavoure@";
    public static final String COPY_COMMAND = "scp";
    public static final String SLAVE_FILENAME = "SLAVE_MR.jar";
    public static final String HOME_FOLDER = "/home/savoga/";

    public static void main(String[] args) throws IOException {
        String fileName = "machines.txt";
        List<String> wordList = getWordList(fileName);

        for (int i = 0; i < wordList.size(); i++) {
            boolean isConnected = false;
            String machineName = wordList.get(i);
            machineName = USER_PREFIX + machineName;
            try {
                // *** Connection to the machine ***
                System.out.println("Trying to connect to " + machineName);
                ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, HOSTNAME_COMMAND);
                MyThread myThread = startThread(processBuilder);
                String valueThread = myThread.getQueue().poll(2, TimeUnit.SECONDS);
                if (valueThread == null) {
                    System.out.println(machineName + " is KO");
                } else {
                    System.out.println(valueThread + " connected");
                    isConnected = true;
                }

                // *** Folder creation ***
                if(isConnected) {
                    while(!folderExist(machineName)) {
                        ProcessBuilder processBuilder2 = new ProcessBuilder(SSH_COMMAND, machineName, FOLDER_CREATION_COMMAND, FOLDER_PATH + FOLDER_NAME);
                        System.out.println("Folder created on machine " + machineName);
                        MyThread myThreadDir = startThread(processBuilder2);
                    }

                    // *** Copy of slave.jar ***
                    ProcessBuilder processBuilder3 = new ProcessBuilder(COPY_COMMAND,  HOME_FOLDER + "/Documents/" + SLAVE_FILENAME, machineName + ":" + FOLDER_PATH + FOLDER_NAME + "/");
                    System.out.println("Copying jar file on " + machineName);
                    MyThread myThreadCopy = startThread(processBuilder3);
                    Thread.sleep(4000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean folderExist(String machineName) {
        ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, LS_COMMAND, FOLDER_PATH);
        MyThread myThread = startThread(processBuilder);
        try {
            Thread.sleep(5000); // let some time so that the queue gets filled
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(myThread.getQueue().contains(FOLDER_NAME)) {
            System.out.println("Folder " + FOLDER_NAME + " found in " + FOLDER_PATH);
            return true;
        };
        System.out.println("No folder named " + FOLDER_NAME);
        return false;
    }

    public static MyThread startThread(ProcessBuilder processBuilder) {
        MyThread myThread = null;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        myThread = new MyThread(queue, processBuilder);
        new Thread(myThread).start();
        return myThread;
    }

    public static List<String> getWordList(String fileName) throws IOException {
        List<String> content = Files.readAllLines(Paths.get(fileName));
        List<String> wordList = new ArrayList<>();
        for (String line : content) { // in this program, line = word
            wordList.add(line);
        }
        return wordList;
    }

}