import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class ErrorThread implements Runnable {

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

	public void setStop(boolean stop) {
		p.destroy();
	}

	private void process() throws InterruptedException, IOException {

		InputStream isErr = p.getErrorStream();
		InputStreamReader isrErr = new InputStreamReader(isErr);
		BufferedReader processOutputErr = new BufferedReader(isrErr);
		String outputErr;

		while ((outputErr = processOutputErr.readLine()) != null) {
			queue.put(outputErr);
		}
	}

	public ErrorThread(BlockingQueue<String> queue, Process p) {
		this.queue = queue;
		this.p = p;
	}
}
