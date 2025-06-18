package com.sofu;

import com.sofu.vtdemulate.MemoryBufferInputStream;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MemoryBufferInputStreamTest {
    private static final String FILE_PATH = "src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml";

    @Test
    public void testGetByteAtVariousOffsets() throws IOException {
        long[] offsets = {10L * 1024 * 1024, 40L * 1024 * 1024, 60L * 1024 * 1024};
        int length = 100;
        try (MemoryBufferInputStream mbis = new MemoryBufferInputStream(FILE_PATH);
             FileInputStream fis = new FileInputStream(new File(FILE_PATH))) {
            byte[] expected = new byte[length];
            for (long offset : offsets) {
                
                fis.getChannel().position(offset);
                int read1 = fis.read(expected, 0, length);
                assertTrue(read1 > 0);
                
                byte[] actual = mbis.getByte(offset, offset + length);
                assertEquals(read1, actual.length);
                
                assertArrayEquals("Mismatch at offset " + offset, Arrays.copyOf(expected, read1), actual);
            }
        }
    }

    @Test
    public void testReadSequentiallyAt45MB() throws IOException {
        long offset = 45L * 1024 * 1024;
        int length = 100;
        try (MemoryBufferInputStream mbis = new MemoryBufferInputStream(FILE_PATH);
             FileInputStream fis = new FileInputStream(new File(FILE_PATH))) {
            
            long skipped = 0;
            while (skipped < offset) {
                int skip = (int)Math.min(1024 * 1024, offset - skipped);
                for (int i = 0; i < skip; i++) {
                    if (mbis.read() == -1) throw new IOException("Unexpected EOF while skipping");
                }
                skipped += skip;
            }
           
            byte[] actual = new byte[length];
            for (int i = 0; i < length; i++) {
                int b = mbis.read();
                if (b == -1) throw new IOException("Unexpected EOF while reading");
                actual[i] = (byte) b;
            }
            
            fis.getChannel().position(offset);
            byte[] expected = new byte[length];
            int read1 = fis.read(expected, 0, length);
            assertEquals(length, read1);
            // 4. 비교
            assertArrayEquals("Mismatch at offset " + offset, expected, actual);
        }
    }

    @org.junit.Test
    public void testReadFirst100BytesOfSimpleXml() throws Exception {
        String filePath = "src/test/java/com/sofu/simple.xml";
        try (MemoryBufferInputStream mbis = new MemoryBufferInputStream(filePath)) {
            byte[] first100 = mbis.getByte(0, 100);
            String text = new String(first100, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("First 100 bytes of simple.xml:\n" + text + ", " + text.getBytes().length);
        }
    }

    @org.junit.Test
    public void testCompareSimpleXmlAndSimpleModifyXml() throws Exception {
        String file1 = "src/test/java/com/sofu/simple.xml";
        String file2 = "src/test/java/com/sofu/simple_modify.xml";
        try (MemoryBufferInputStream mbis1 = new MemoryBufferInputStream(file1);
             MemoryBufferInputStream mbis2 = new MemoryBufferInputStream(file2)) {
            long size1 = mbis1.size();
            long size2 = mbis2.size();
            org.junit.Assert.assertEquals("File sizes differ", size1, size2);
            int chunk = 4096;
            long offset = 0;
            while (offset < size1) {
                long end = Math.min(offset + chunk - 1, size1 - 1);
                byte[] b1 = mbis1.getByte(offset, end);
                byte[] b2 = mbis2.getByte(offset, end);
                org.junit.Assert.assertArrayEquals("Mismatch at offset " + offset, b1, b2);
                offset += chunk;
            }
            System.out.println("simple.xml and simple_modify.xml are identical.");
        }
    }
} 