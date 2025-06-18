package com.sofu;

import com.sofu.vtdemulate.StaxXmlAdapter;
import org.junit.Test;

public class StaxXmlAdapterThreadTest {
    @Test
    public void testThreadedPauseResumeParsing() throws Exception {
        // Use a sufficiently large XML file for block boundary demonstration
        String filePath = "src/test/java/com/sofu/simple.xml";
        StaxXmlAdapter adapter = new StaxXmlAdapter(filePath);

        // Start parsing in a separate thread
        System.out.println("Starting parsing in a separate thread...");
        adapter.startParsing();

        // Wait for a short time to let parsing begin
        Thread.sleep(500);

        // Wait to ensure the parser has a chance to hit a block boundary and pause
        Thread.sleep(1000);
        System.out.println("Is parsing paused? " + adapter.isPaused());

        // Resume parsing
        System.out.println("Resuming parsing...");
        adapter.resumeParsingAndWait(2000);

        // Wait for parsing to finish
        while (adapter.isRunning()) {
            Thread.sleep(200);
        }
    }
} 