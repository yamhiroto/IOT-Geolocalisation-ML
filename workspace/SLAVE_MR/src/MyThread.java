import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class MyThread implements Runnable {

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

    public BlockingQueue<String> getQueue() {
        return queue;
    }

    public MyThread(BlockingQueue<String> queue, ProcessBuilder processBuilder) {
        Process p = null;
        try {
            p = processBuilder.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.queue = queue;
        this.p = p;
    }
}
