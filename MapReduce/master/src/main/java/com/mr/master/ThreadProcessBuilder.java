package com.mr.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadProcessBuilder implements Runnable {

    private BlockingQueue<String> queue = new LinkedBlockingQueue<>();;
    private Process p;
    private boolean _processFinished;

    @Override
    public void run() {
        try {
            _processFinished = false;
            process();
            _processFinished = true;
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setStop(Boolean stop) {
        p.destroy();
    }

    public boolean isProcessFinished() {
        return _processFinished;
    }

    private void process() throws InterruptedException, IOException {

        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader processOutput = new BufferedReader(isr);
        String output;

        while ((output = processOutput.readLine()) != null) { // WARNING: this line seems to be executed 3 TIMES when ONE thread is launched from the main
            queue.put(output);
        }

    }

    public BlockingQueue<String> getQueue() {
        return queue;
    }

    public ThreadProcessBuilder(ProcessBuilder processBuilder) {
        Process p = null;
        try {
            p = processBuilder.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.p = p;
    }
}
