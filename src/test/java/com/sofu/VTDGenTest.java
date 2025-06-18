package com.sofu;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.sofu.vtdemulate.MutableXmlElement;
import com.sofu.vtdemulate.VTDGen;
import com.sofu.vtdemulate.XmlElement;

public class VTDGenTest {
    @Test
    public void testGetChildren() {
        VTDGen gen = new VTDGen();
        boolean parsed = gen.parseFile("src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml");
        assertTrue(parsed);
        List<MutableXmlElement> children = gen.getChildren();
        assertNotNull(children);
        // At least one child node should exist (adjust according to sample XML structure)
        assertTrue(children.size() > 0);
        for (XmlElement child : children) {
            System.out.println("Root child: " + child.getTagName() + ", start=" + child.getStartOffset() + ", end=" + child.getEndOffset());
        }
    }

    @Test
    public void testGetParent() {
        VTDGen gen = new VTDGen();
        boolean parsed = gen.parseFile("src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml");
        assertTrue(parsed);
        // getParent currently returns false, so only check behavior
        assertFalse(gen.getParent());
    }

    @Test
    public void testMeContextCountAfterParse() {
        VTDGen gen = new VTDGen();
        boolean parsed = gen.parseFile("src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml");
        assertTrue(parsed);
        List<MutableXmlElement> roots = gen.getChildren();
        int count = 0;
        for (XmlElement root : roots) {
            count += countMeContextRecursive(root);
        }
        System.out.println("MeContext XmlElement count after parse: " + count);
    }

    private int countMeContextRecursive(XmlElement node) {
        int cnt = 0;
        if ("MeContext".equals(node.getTagName())) { 
            cnt++;
            return cnt;
        }
        for (XmlElement child : node.getChildren()) {
            cnt += countMeContextRecursive(child);
        }
        return cnt;
    }
} 