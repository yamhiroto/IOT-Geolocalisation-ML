import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    public static final String FOLDER_NAME_PERSO = "/savoga";
    //public static final String FOLDER_NAME_PERSO = "/gsavoure";
    public static final String FOLDER_CREATION_COMMAND = "mkdir";

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
                    ProcessBuilder processBuilder2 = new ProcessBuilder(FOLDER_CREATION_COMMAND, "-p", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS);
                    // TODO: replace ProcessBuilder with FileWriter
                    System.out.println("Folder /maps created");
                    MyThread myThreadDir = startThread(processBuilder2);
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
                // *** SHUFFLE: Write the hash files ***

                while (!folderExist(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES)) {
                    ProcessBuilder processBuilder2 = new ProcessBuilder(FOLDER_CREATION_COMMAND, "-p", FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES);
                    System.out.println("Folder /shuffles created");
                    MyThread myThreadDir = startThread(processBuilder2);
                    Thread.sleep(2000); // let some time for the folder to be created
                }

                List<String> contentMapFile = Files.readAllLines(Paths.get(args[1]));
                for (String line : contentMapFile) {
                    String[] words = line.split(" ");
                    String word = words[0];
                    String hashCodeWord = Integer.toString(word.hashCode());
                    String hostNameMachine = java.net.InetAddress.getLocalHost().getHostName();
                    String fileNameShuffle = hashCodeWord + "-" + hostNameMachine + ".txt";
                    System.out.println(fileNameShuffle);

                    File fileText = new File(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES + "/" + fileNameShuffle);
                    FileOutputStream is = new FileOutputStream(fileText, true);
                    OutputStreamWriter osw = new OutputStreamWriter(is);

                    if (!folderExist(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_SHUFFLES + "/" + fileNameShuffle)) {
                        fileText.createNewFile();
                    }
                    Writer w = new BufferedWriter(osw);
                    w.append(word + " 1");
                    w.append("\n\r");
                    w.close();
                }

                break;
        }

    }

    public static boolean folderExist(String searchedFolder) throws InterruptedException {
        Path p = Paths.get(searchedFolder);
        return Files.exists(p);
    }

    public static MyThread startThread(ProcessBuilder processBuilder) {
        MyThread myThread = null;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        myThread = new MyThread(queue, processBuilder);
        new Thread(myThread).start();
        return myThread;
    }
}