import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/***
 * @author gsavoure
 *
 * Question 1. STEP 10
 * Question 3 STEP 10
 *
 */

public class MASTER_MR {

    public static final String FOLDER_NAME_TMP = "/tmp";
    public static final String FOLDER_NAME_SPLITS = "/splits";
    public static final String FOLDER_NAME_PERSO = "/savoga";
    public static final String SSH_COMMAND = "ssh";
    public static final String FOLDER_CREATION_COMMAND = "mkdir -p";
    public static final String HOSTNAME_COMMAND = "hostname";
    public static final String SLAVE_FILENAME = "SLAVE_MR.jar";
    public static final String USER_PREFIX = "gsavoure@";
    public static final String COPY_COMMAND = "scp";
    public static final String HOME_FOLDER = "/home/savoga";
    public static List<String> usedMachines = new ArrayList<>();
    public static final int nbFiles = 2;

    public static void main(String[] args) throws Exception {

        System.out.println("*** Splits deployment started. ***");
        split_deploy_MR();
        usedMachines.clear();
        System.out.println("*** Splits deployment finished. ***");
        System.out.println("*** Map started. ***");
        map_MR();
        System.out.println("*** Map finished. ***");

    }

    private static void map_MR() throws IOException, InterruptedException {
        for(int i=0; i<nbFiles; i++) {
            String machineName = getNextAvailableMachine();
            ProcessBuilder processBuilder2 = new ProcessBuilder(SSH_COMMAND, machineName, "java", "-jar", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + "/" + SLAVE_FILENAME,
                    "0", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SPLITS + "/" + "S" + i + ".txt");
            System.out.println("map executed on machine " + machineName);
            ThreadProcessBuilder threadProcessBuilderDir = startThreadProcessBuilder(processBuilder2);
            Thread.sleep(2000); // let some time for the folder to be created
        }
    }

    private static void split_deploy_MR() throws Exception {
        Thread[] threads = new Thread[nbFiles];
        for(int i=0; i<nbFiles; i++) {
            String splitName = "S" + i + ".txt";
            Path p = Paths.get(splitName);
            if(p!=null) {
                String machineName = getNextAvailableMachine();
                if(machineName == null) {
                    throw new Exception("No machine available!");
                }
                ThreadProcessBuilder threadProcessBuilderDir = null;
                // *** Folder '/splits' creation ***
                while(!folderExist(machineName, FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SPLITS)) {
                    ProcessBuilder processBuilder2 = new ProcessBuilder(SSH_COMMAND, machineName, FOLDER_CREATION_COMMAND, FOLDER_NAME_TMP + FOLDER_NAME_PERSO + "/splits");
                    System.out.println("Folder created on machine " + machineName);
                    threadProcessBuilderDir = startThreadProcessBuilder(processBuilder2);
                }

                // *** Copy of split files ***
                ProcessBuilder processBuilder3 = new ProcessBuilder(COPY_COMMAND,  HOME_FOLDER + "/Documents/workspace/MASTER_MR/" + splitName, machineName + ":" + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SPLITS + "/");
                System.out.println("Copying split file on " + machineName);
                ThreadProcessBuilder threadProcessBuilderCopy = startThreadProcessBuilder(processBuilder3);
            }
        }
    }

    public static String getNextAvailableMachine() throws IOException {
        String fileName = "machines_test.txt";
        List<String> wordList = getWordList(fileName);

        for (int i = 0; i < wordList.size(); i++) {
            String machineName = wordList.get(i);
            machineName = USER_PREFIX + machineName;
            if(usedMachines.contains(machineName)) {
                continue;
            }
            try {
                // *** Connection to the machine ***
                System.out.println("Trying to connect to " + machineName);
                ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, HOSTNAME_COMMAND);
                ThreadProcessBuilder threadProcessBuilder = startThreadProcessBuilder(processBuilder);
                String valueThread = threadProcessBuilder.getQueue().poll(2, TimeUnit.SECONDS);
                if (valueThread == null) {
                    System.out.println(machineName + " is KO");
                } else {
                    System.out.println(valueThread + " connected");
                    usedMachines.add(machineName);
                    return machineName;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    public static boolean folderExist(String machineName, String searchedFolder) throws InterruptedException {
        // Copy of a script that looks for a specific folder
        ProcessBuilder processBuilderScript = new ProcessBuilder(COPY_COMMAND,  HOME_FOLDER + "/fileSearch.sh", machineName + ":" + "/tmp");
        ThreadProcessBuilder thread_ProcessBuilder_1 = startThreadProcessBuilder(processBuilderScript);

        // Execution of the script
        ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, "./fileSearch.sh", searchedFolder);
        ThreadProcessBuilder thread_ProcessBuilder_2 = startThreadProcessBuilder(processBuilder);

        if(!thread_ProcessBuilder_2.getQueue().poll(1, TimeUnit.SECONDS).contains("not found")) {
            System.out.println("Folder " + searchedFolder + " found on machine " + machineName);
            return true;
        };
        System.out.println("No folder named " + searchedFolder);
        return false;
    }

    public static ThreadProcessBuilder startThreadProcessBuilder(ProcessBuilder processBuilder) {
        ThreadProcessBuilder threadProcessBuilder = null;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        threadProcessBuilder = new ThreadProcessBuilder(queue, processBuilder);
        new Thread(threadProcessBuilder).start();
        return threadProcessBuilder;
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