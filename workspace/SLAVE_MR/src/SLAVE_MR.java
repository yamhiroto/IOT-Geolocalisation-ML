import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SLAVE_MR {

    public static final String FOLDER_NAME_TMP = "/tmp";
    public static final String FOLDER_NAME_MAPS = "/maps";
    public static final String FOLDER_NAME_SHUFFLES = "/shuffles";
    public static final String FOLDER_NAME_SHUFFLESRECEIVED = "/shufflesreceived";
    public static final String FOLDER_NAME_PERSO = "/savoga";
    //public static final String FOLDER_NAME_PERSO = "/gsavoure";
    public static final String FOLDER_CREATION_COMMAND = "mkdir";
    public static final String FILENAME_MACHINES = "machines_test.txt";
    public static final String COPY_COMMAND = "scp";
    public static final String HOME_FOLDER = "/home/savoga";
    public static final String SSH_COMMAND = "ssh";
    public static final String USER_PREFIX = "gsavoure@";

    public static void main(String[] args) throws Exception {

        // WARNING: DO NOT REPLACE THE "-p" BY A VARIABLE IN THE PROCESSBUILDER

        if (args.length == 0) {
            throw new Exception("No argument!!");
        }

        switch (args[0]) {

            case "0":
                // *** MAP: Write the UM file ***

                int dotIndex = args[1].indexOf('.');
                String splitNb = args[1].substring(dotIndex - 1, dotIndex);

                Path mapFile = Paths.get(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS + "/UM" + splitNb + ".txt");

                List<String> contentSplitFile = Files.readAllLines(Paths.get(args[1]));
                Map<String, Integer> mapOccurences = new TreeMap<>();
                List<String> listOccurences = new ArrayList<>();

                while (!folderExist(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS)) {
                    ProcessBuilder processBuilder = new ProcessBuilder(FOLDER_CREATION_COMMAND, "-p", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS);
                    // TODO: replace ProcessBuilder with FileWriter
                    System.out.println("Folder /maps created");
                    ThreadProcessBuilder threadProcessBuilderDir = startThread(processBuilder);
                    Thread.sleep(2000); // let some time for the folder to be created
                }

                for (String line : contentSplitFile) {
                    String[] words = line.split(" ");
                    for (String word : words) {
                        //mapOccurences.put(word, 1);
                        listOccurences.add(word + " 1");
                    }
                }
                Files.write(mapFile, listOccurences, StandardCharsets.UTF_8);
                System.out.println("UM" + splitNb + " created in " + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS);
                break;

            case "1":
                // *** SHUFFLE: Write the hash files and send them to different machines ***
                List machineList = getMachineList();
                int nbMachines = machineList.size();
                System.out.println(nbMachines + " machines will be used");
                createFilesWithHashCode(args[1]); // Parameter is the Umap file
                sendFilesOnMachines(machineList, nbMachines);
                break;
        }
    }

    private static void sendFilesOnMachines(List machineList, int nbMachines) throws InterruptedException {
        List<String> fileNameList = getListFilesForFolder(new File(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES));

        for (String fileName : fileNameList) {
            String hashCode = fileName.split("-")[0];
            int machineIndex = Integer.parseInt(hashCode) % nbMachines;
            String machineName = USER_PREFIX + machineList.get(machineIndex);

            while (!distantFolderExist(machineName, FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLESRECEIVED)) {
                ProcessBuilder pb = new ProcessBuilder(SSH_COMMAND, machineName, FOLDER_CREATION_COMMAND, "-p", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLESRECEIVED);
                System.out.println("Folder " + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLESRECEIVED + " created on machine " + machineName);
                startThread(pb);
            }
            ProcessBuilder pb = new ProcessBuilder(COPY_COMMAND, FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES + "/" + fileName,
                    machineName + ":" + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLESRECEIVED);
            System.out.println("File " + fileName + " sent on machine " + machineName);
            startThread(pb);
        }
    }

    private static void createFilesWithHashCode(String arg) throws InterruptedException, IOException {
        while (!folderExist(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES)) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(FOLDER_CREATION_COMMAND, "-p", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES);
            System.out.println("Folder /shuffles created");
            ThreadProcessBuilder threadProcessBuilderDir = startThread(processBuilder2);
            Thread.sleep(2000); // let some time for the folder to be created
        }

        List<String> contentMapFile = Files.readAllLines(Paths.get(arg));
        for (String line : contentMapFile) {
            String[] words = line.split(" ");
            String word = words[0];
            String hashCodeWord = Integer.toString(word.hashCode());
            String hostNameMachine = InetAddress.getLocalHost().getHostName();
            String fileNameShuffle = hashCodeWord + "-" + hostNameMachine + ".txt";

            // File creation
            File fileText = new File(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES + "/" + fileNameShuffle);
            FileOutputStream is = new FileOutputStream(fileText, true); // create new file or append if existing
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.append(word + " 1");
            w.append("\n");
            w.close();
        }
    }

    public static List getListFilesForFolder(final File folder) {
        List<String> fileNameList = new ArrayList();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                getListFilesForFolder(fileEntry);
            } else {
                String fileName = fileEntry.getName();
                fileNameList.add(fileName);
            }
        }
        return fileNameList;
    }

    public static List<String> getMachineList() throws IOException {
        List<String> content = Files.readAllLines(Paths.get(FILENAME_MACHINES));
        List<String> wordList = new ArrayList<>();
        for (String line : content) { // in this program, line = word
            wordList.add(line);
        }
        return wordList;
    }

    public static boolean folderExist(String searchedFolder) {
        Path p = Paths.get(searchedFolder);
        return Files.exists(p);
    }

    public static boolean distantFolderExist(String machineName, String searchedFolder) throws InterruptedException {
        // TODO: check how to share this method with MASTER program
        // Copy of a script that looks for a specific folder
        ProcessBuilder processBuilderScript = new ProcessBuilder(COPY_COMMAND, HOME_FOLDER + "/fileSearch.sh", machineName + ":" + "/tmp");
        ThreadProcessBuilder thread_ProcessBuilder_1 = startThread(processBuilderScript);

        // Execution of the script
        ProcessBuilder processBuilder = new ProcessBuilder(SSH_COMMAND, machineName, "./fileSearch.sh", searchedFolder);
        ThreadProcessBuilder thread_ProcessBuilder_2 = startThread(processBuilder);

        if (thread_ProcessBuilder_2.getQueue().take().contains("not found")) {
            // WARNING: take() is used in order to wait for the queue to be filled (for some reasons, the
            // same queue is used for the two threads of this method; thus, it does not get filled with the first thread...)
            System.out.println("No folder named " + searchedFolder + " on machine " + machineName);
            return false;
        }
        System.out.println("Folder " + searchedFolder + " found on machine " + machineName);
        return true;
    }


    public static ThreadProcessBuilder startThread(ProcessBuilder processBuilder) {
        ThreadProcessBuilder threadProcessBuilder = null;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        threadProcessBuilder = new ThreadProcessBuilder(queue, processBuilder);
        new Thread(threadProcessBuilder).start();
        return threadProcessBuilder;
    }
}