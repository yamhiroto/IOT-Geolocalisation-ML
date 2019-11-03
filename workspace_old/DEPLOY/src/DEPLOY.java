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
 * Question 1. STEP 7
 *
 */

public class DEPLOY {

	public static void main(String[] args) throws IOException {
		String fileName = "machines.txt";
		List<String> wordList = getWordList(fileName);

		for (int i = 0; i < wordList.size(); i++) {
			String machineName = wordList.get(i);
			System.out.println("Trying to connect to " + machineName);
			ProcessBuilder processBuilder = new ProcessBuilder("ssh", "-tt", machineName, "hostname");
			try {
				BlockingQueue<String> queue = new LinkedBlockingQueue<>();
				Process p = processBuilder.start();
				MyThread myThread = new MyThread(queue, p);
				new Thread(myThread).start();
				String valueThread = queue.poll(2, TimeUnit.SECONDS);
				if (valueThread == null) {
					System.out.println(machineName + " is KO");
				} else {
					System.out.println(valueThread + " connected");
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
