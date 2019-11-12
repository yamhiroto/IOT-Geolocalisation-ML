import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    public static final String MACHINES_FILENAME="machines_test.txt";
    public static final int nbFiles = 3;

    public static void main(String[] args) throws Exception {

        System.out.println("*** Split deployment started. ***");
        split_deploy_MR();
        usedMachines.clear();
        System.out.println("*** Split deployment finished. ***");

        System.out.println("*** Map started. ***");
        map_MR();
        usedMachines.clear();
        System.out.println("*** Map finished. ***");

        System.out.println("*** Shuffle started. ***");
        shuffle_MR();
        System.out.println("*** Shuffle finished. ***");

    }

    private static void shuffle_MR() throws IOException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadShuffle_sendTxtMachines[] threads = new ThreadShuffle_sendTxtMachines[20];
        String machineName;
        int i =0;
        while((machineName = getNextAvailableMachine()) != null){
            threads[i] = new ThreadShuffle_sendTxtMachines(machineName);
            es.execute(threads[i]);
            i++;
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println(MACHINES_FILENAME + " sent on all machines.");
        }
    }

    private static void map_MR() throws IOException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadMap[] threads = new ThreadMap[nbFiles];
        for (int i = 0; i < nbFiles; i++) {
            String splitName = "S" + i + ".txt";
            threads[i] = new ThreadMap(getNextAvailableMachine(), splitName);
            es.execute(threads[i]);
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("All map threads terminated.");
        }
    }

    private static void split_deploy_MR() throws Exception {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadDeploySplit[] threads = new ThreadDeploySplit[nbFiles];
        for (int i = 0; i < nbFiles; i++) {
            String splitName = "S" + i + ".txt";
            Path p = Paths.get(splitName);
            if (p != null) {
                threads[i] = new ThreadDeploySplit(getNextAvailableMachine(), splitName);
                es.execute(threads[i]);
            }
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("All split threads terminated.");
        }
    }

    public static String getNextAvailableMachine() throws IOException {
        List<String> wordList = getWordList(MACHINES_FILENAME);

        for (int i = 0; i < wordList.size(); i++) {
            String machineName = wordList.get(i);
            machineName = USER_PREFIX + machineName;
            if (usedMachines.contains(machineName)) {
                continue;
            }
            try {
                // *** Connection to the machine ***
                System.out.println("Trying to connect to " + machineName);
                ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, HOSTNAME_COMMAND);
                ThreadProcessBuilder threadProcessBuilder = startThreadProcessBuilder(processBuilder);
                Thread.sleep(3000);
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
        ProcessBuilder processBuilderScript = new ProcessBuilder(COPY_COMMAND, HOME_FOLDER + "/fileSearch.sh", machineName + ":" + "/tmp");
        ThreadProcessBuilder thread_ProcessBuilder_1 = startThreadProcessBuilder(processBuilderScript);

        // Execution of the script
        ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, "./fileSearch.sh", searchedFolder);
        ThreadProcessBuilder thread_ProcessBuilder_2 = startThreadProcessBuilder(processBuilder);

        if (thread_ProcessBuilder_2.getQueue().take().contains("not found")) {
            // WARNING: take() is used in order to wait for the queue to be filled (for some reasons, the
            // same queue is used for the two threads of this method; thus, it does not get filled with the first thread...)
            System.out.println("No folder named " + searchedFolder + " on machine " + machineName);
            return false;
        }
        System.out.println("Folder " + searchedFolder + " found on machine " + machineName);
        return true;
    }

    public static ThreadProcessBuilder startThreadProcessBuilder(ProcessBuilder processBuilder) {
        ThreadProcessBuilder threadProcessBuilder = new ThreadProcessBuilder(processBuilder);
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