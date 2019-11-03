import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class CleanThread implements Runnable {

	private final BlockingQueue<String> queue;
	private Process p;

	@Override
	public void run() {
		try {
			process();
		} catch (InterruptedException | IOException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void setStop(Boolean stop) {
		p.destroy();
	}

	private void process() throws InterruptedException, IOException {

		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader processOutput = new BufferedReader(isr);
		String output;

		while ((output = processOutput.readLine()) != null) {
			queue.put(output);
		}
	}

	public CleanThread(BlockingQueue<String> queue, Process p) {
		this.queue = queue;
		this.p = p;
	}
}
