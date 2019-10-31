import java.io.IOException;
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
	public static final String FOLDER_NAME_PERSO = "/gsavoure";
	public static final String FOLDER_CREATION_COMMAND = "mkdir";
	public static final String HOME_FOLDER = "/cal/homes";
	
	public static void main(String[] args) throws IOException, InterruptedException {

		int dotIndex = args[1].indexOf('.');
		String splitNb = args[1].substring(dotIndex-1, dotIndex);
		
		Path mapFile = Paths.get(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS + "/UM" + splitNb +  ".txt");
	
		List<String> content = Files.readAllLines(Paths.get(args[1]));
		Map<String, Integer> mapOccurences = new TreeMap<>();
		List<String> listOccurences = new ArrayList<>();

		while(!folderExist(FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS)) {
			ProcessBuilder processBuilder2 = new ProcessBuilder(FOLDER_CREATION_COMMAND, FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS);
			System.out.println("Folder /maps created");
			MyThread myThreadDir = startThread(processBuilder2);
			Thread.sleep(2000); // let some time for the folder to be created
		}

		for(String line: content) {
			String[] words = line.split(" ");
			for(String word: words) {
				//mapOccurences.put(word, 1);
				listOccurences.add(word + " 1");
			}
		}
		Files.write(mapFile, listOccurences, StandardCharsets.UTF_8);
		System.out.println("UM" + splitNb + " created in " + FOLDER_NAME_TMP + FOLDER_NAME_PERSO + FOLDER_NAME_MAPS);
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
