import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MASTER {

	public static final String SSH_COMMAND = "ssh";
	public static final String machineName = "c125-11";
	
	public static void main(String[] args) throws InterruptedException {
			
		ProcessBuilder processBuilder = new ProcessBuilder("./fileScript.sh", "/tmp/gsavoure", "c125-11");
		//"/tmp/gsavoure/SLAVE_GS.jar");
		//"/cal/homes/gsavoure/SLAVE.jar");
		try {

			BlockingQueue<String> queue = new LinkedBlockingQueue<>();

			Process p = processBuilder.start();
			CleanThread cleanThread = new CleanThread(queue, p);
			ErrorThread errorThread = new ErrorThread(queue, p);
			new Thread(cleanThread).start();
			new Thread(errorThread).start();

			String valueThread = queue.poll(15, TimeUnit.SECONDS);
			if (valueThread == null) {
				System.out.println("no value returned; killing process");
			} else {
				System.out.println("value returned is: " + valueThread);
			}

			cleanThread.setStop(true);
			errorThread.setStop(true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
