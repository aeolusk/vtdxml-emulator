package com.sofu;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sofu.vtdemulate.MemoryBufferInputStream;
import com.sofu.vtdemulate.MutableXmlElement;
import com.sofu.vtdemulate.XmlElement;

public class TagPositionWriteTest {
    private static final String SRC = "src/test/java/com/sofu/simple.xml";
    private static final String COPY = "src/test/java/com/sofu/simple_copy.xml";

    @Test
    public void testTagPositionWriteHierarchyAndCompare() throws IOException {
        File srcFile = new File(SRC);
        long fileSize = srcFile.length();
        try (MemoryBufferInputStream mbis = new MemoryBufferInputStream(SRC);
             OutputStream out = new FileOutputStream(COPY)) {
            // 1. Create a 3-level TagPosition tree (split by 1/10 each)
            XmlElement root = new MutableXmlElement();
            root.setStartOffset(0);
            root.setEndOffset(fileSize);
            List<XmlElement> level1 = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                long l1Start = i * fileSize / 10;
                long l1End = (i == 9) ? fileSize : (i + 1) * fileSize / 10;
                XmlElement l1 = new MutableXmlElement();
                l1.setStartOffset(l1Start);
                l1.setEndOffset(l1End);
                root.addChild(l1);
                // 2nd level: split each 1/10 into 2
                List<XmlElement> level2 = new ArrayList<>();
                for (int j = 0; j < 2; j++) {
                    long l2Start = l1Start + j * (l1End - l1Start) / 2;
                    long l2End = (j == 1) ? l1End : l2Start + (l1End - l1Start) / 2;
                    XmlElement l2 = new MutableXmlElement();
                    l2.setStartOffset(l2Start);
                    l2.setEndOffset(l2End);
                    l1.addChild(l2);
                    // 3rd level: split each 1/20 into 2
                    for (int k = 0; k < 2; k++) {
                        long l3Start = l2Start + k * (l2End - l2Start) / 2;
                        long l3End = (k == 1) ? l2End : l3Start + (l2End - l2Start) / 2;
                        XmlElement l3 = new MutableXmlElement();
                        l3.setStartOffset(l3Start);
                        l3.setEndOffset(l3End);
                        l2.addChild(l3);
                    }
                }
            }
            // 2. Create a copy using write
            root.write(out, mbis);
        }
        // 3. Compare original and copy
        try (InputStream orig = new FileInputStream(SRC);
             InputStream copy = new FileInputStream(COPY)) {
            int b1, b2;
            long pos = 0;
            while ((b1 = orig.read()) != -1) {
                b2 = copy.read();
                assertEquals("Mismatch at byte " + pos, b1, b2);
                pos++;
            }
            assertEquals(-1, copy.read()); // Copy should also be at the end
        }
        // 4. Delete the copy (uncomment after verification)
        new File(COPY).delete();
    }
} 