package com.mr.master;

import java.io.FileWriter;
import java.util.List;

public class ThreadCreateSplit implements Runnable {

    private int _splitNumber;
    private List<String> _chunk;

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process() throws Exception {

        FileWriter writer = new FileWriter(Master.FOLDER_RESOURCES + "/" + "S" + _splitNumber + ".txt");
        int countWord = 0;
        for (int i = 0; i < _chunk.size(); i++) {
            String str = _chunk.get(i);
            if ("".equalsIgnoreCase(str)) continue;
            writer.write(str.replace(",", "").replace(" ", ""));
            if (i < _chunk.size() - 1) {
                if (countWord == 20) { // 20 words by line max
                    writer.write("\n");
                }
                writer.write(" ");
                countWord += 1;
            }
        }
        writer.close();
    }


    public ThreadCreateSplit(List chunk, int splitNumber) {
        _chunk = chunk;
        _splitNumber = splitNumber;
    }

}

