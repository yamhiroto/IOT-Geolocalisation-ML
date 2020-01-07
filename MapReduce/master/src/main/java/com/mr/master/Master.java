package com.mr.master;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.text.DecimalFormat;
import java.util.stream.Stream;

/***
 * @author gsavoure
 */

public class Master {

    public static final String FOLDER_NAME_TMP = "/tmp";
    public static final String FOLDER_NAME_SPLITS = "/splits";
    public static final String FOLDER_NAME_REDUCES = "/reduces";
    public static final String FOLDER_NAME_PERSO = "/savoga";
    public static final String FOLDER_NAME_MAPS = "/maps";
    public static final String SSH_COMMAND = "ssh";
    public static final String FOLDER_CREATION_COMMAND = "mkdir -p";
    public static final String HOSTNAME_COMMAND = "hostname";
    public static final String SLAVE_FILENAME = "SLAVE_MR.jar";
    public static final String USER_PREFIX = "gsavoure@";
    public static final String COPY_COMMAND = "scp";
    public static final String FOLDER_RESOURCES = "src/main/resources";
    public static final String HOME_FOLDER = "/home/savoga";
    public static final String LS_COMMAND = "ls";
    public static final String INPUT_FILE = "input.txt";
    public static List<String> usedMachines = new ArrayList<>();
    public static final String MACHINES_FILENAME = "machines.txt";
    public static final int nbSplitFiles = 3;

    public static void main(String[] args) throws Exception {

        long startTimeSplit = System.nanoTime();
        System.out.println("*** Split started. ***");
        split_create_MR();
        split_deploy_MR();
        usedMachines.clear();
        System.out.println("*** Split finished. ***");
        long endTimeSplit = System.nanoTime();

        long startTimeMap = System.nanoTime();
        System.out.println("*** Map started. ***");
        map_MR();
        usedMachines.clear();
        System.out.println("*** Map finished. ***");
        long endTimeMap = System.nanoTime();

        long startTimeShuffle = System.nanoTime();
        System.out.println("*** Shuffle started. ***");
        shuffle_MR();
        usedMachines.clear();
        System.out.println("*** Shuffle finished. ***");
        long endTimeShuffle = System.nanoTime();

        long startTimeReduce = System.nanoTime();
        System.out.println("*** Reduce started. ***");
        reduce_MR();
        usedMachines.clear();
        System.out.println("*** Reduce finished. ***");
        long endTimeReduce = System.nanoTime();

        long startTimeConcatenation = System.nanoTime();
        System.out.println("*** Concatenation started. ***");
        concatenate_results();
        System.out.println("*** Concatenation finished. ***");
        long endTimeConcatenation = System.nanoTime();

        System.out.println("Split running time: " + formatElapsedTime(startTimeSplit, endTimeSplit));
        System.out.println("Map running time: " + formatElapsedTime(startTimeMap, endTimeMap));
        System.out.println("Shuffle running time: " + formatElapsedTime(startTimeShuffle, endTimeShuffle));
        System.out.println("Reduce running time: " + formatElapsedTime(startTimeReduce, endTimeReduce));
        System.out.println("Concatenation running time: " + formatElapsedTime(startTimeConcatenation, endTimeConcatenation));
    }

    private static void concatenate_results() throws IOException, InterruptedException {
        List<String> wordList = getWordList(FOLDER_RESOURCES + "/" + MACHINES_FILENAME);
        for (int i = 0; i < wordList.size(); i++) {
            String machineName = wordList.get(i);
            machineName = USER_PREFIX + machineName;
            ProcessBuilder processBuilder = new ProcessBuilder(COPY_COMMAND, "-r", machineName + ":" + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_REDUCES, HOME_FOLDER);
            System.out.println("Copying all reduce files locally from " + machineName);
            ThreadProcessBuilder t = startThreadProcessBuilder(processBuilder);
            while (!t.isProcessFinished()) {
                Thread.sleep(2000);
            }
        }
        Thread.sleep(4000);
        File dir = new File(HOME_FOLDER + FOLDER_NAME_REDUCES);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                StringBuilder contentBuilder = new StringBuilder();
                Stream<String> stream = Files.lines(Paths.get(child.getPath()), StandardCharsets.UTF_8);
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
                File fileText = new File(HOME_FOLDER + "/" + "MR_results.txt");
                FileOutputStream is = new FileOutputStream(fileText, true); // create new file or append if existing
                OutputStreamWriter osw = new OutputStreamWriter(is);
                Writer w = new BufferedWriter(osw);
                w.append(contentBuilder.toString());
                w.close();
            }
        }

    }

    private static void split_create_MR() throws InterruptedException, IOException {
        StringBuilder contentBuilder = new StringBuilder();
        Stream<String> stream = Files.lines(Paths.get(FOLDER_RESOURCES + "/" + INPUT_FILE), StandardCharsets.UTF_8);
        stream.forEach(s -> contentBuilder.append(s).append(" "));
        List<String> fileWords = Arrays.asList(contentBuilder.toString().split(" "));
        int chunkSize = (int) Math.ceil((double) fileWords.size() / nbSplitFiles);
        Partition p = Partition.ofSize(fileWords, chunkSize);

        ExecutorService es = Executors.newCachedThreadPool();
        ThreadCreateSplit[] threads = new ThreadCreateSplit[nbSplitFiles];
        for (int i = 0; i < nbSplitFiles; i++) {
            threads[i] = new ThreadCreateSplit(p.get(i), i);
            es.execute(threads[i]);
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("Split files created.");
        }
    }

    private static String formatElapsedTime(long startTime, long endTime) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format((double) (endTime - startTime) / 1_000_000_000) + "s";
    }

    private static void reduce_MR() throws IOException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadReduce[] threads = new ThreadReduce[20];
        String machineName;
        int i = 0;
        while ((machineName = getNextAvailableMachine()) != null) {
            threads[i] = new ThreadReduce(machineName);
            es.execute(threads[i]);
            i++;
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("All reduce threads terminated.");
        }
    }

    private static void shuffle_MR() throws IOException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadShuffle[] threads = new ThreadShuffle[20];
        String machineName;
        int i = 0;
        while ((machineName = getNextAvailableMachine()) != null) {
            threads[i] = new ThreadShuffle(machineName);
            es.execute(threads[i]);
            i++;
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("All shuffle threads terminated.");
        }
    }

    private static void map_MR() throws IOException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        ThreadMap[] threads = new ThreadMap[nbSplitFiles];
        for (int i = 0; i < nbSplitFiles; i++) {
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
        ThreadDeploySplit[] threads = new ThreadDeploySplit[nbSplitFiles];
        for (int i = 0; i < nbSplitFiles; i++) {
            String splitName = "S" + i + ".txt";
            File f = new File(FOLDER_RESOURCES + "/" + splitName);
            if (f.exists()) {
                threads[i] = new ThreadDeploySplit(getNextAvailableMachine(), splitName);
                es.execute(threads[i]);
            } else {
                System.out.println("no split file found");
            }
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            System.out.println("All split threads terminated.");
        }
    }

    public static String getNextAvailableMachine() throws IOException {
        List<String> wordList = getWordList(FOLDER_RESOURCES + "/" + MACHINES_FILENAME);

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
                Thread.sleep(8000);
                String valueThread = threadProcessBuilder.getQueue().poll(3, TimeUnit.SECONDS);
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