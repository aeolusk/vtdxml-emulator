package com.sofu;

import com.sofu.vtdemulate.StaxXmlAdapter;
import com.sofu.vtdemulate.VTDNav;
import com.sofu.vtdemulate.XmlElement;
import com.sofu.vtdemulate.MutableXmlElement;
import org.junit.Test;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sofu.vtdemulate.MemoryBufferInputStream;

public class StaxXmlAdapterTest {
    int meContextCount = 0;
    @Test
    public void testPrintMeContextTags() throws IOException, XMLStreamException {
        System.out.println("Start test[testPrintMeContextTags]");
        String filePath = "src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml";
        StaxXmlAdapter adapter = new StaxXmlAdapter(filePath);
        adapter.parseXml(false);
        
        meContextCount = 0;
        for (MutableXmlElement node : adapter.getRootElements()) {
        	searchMeContextRecursive(node);
        }
        System.out.println("Total xn:MeContext tags: " + meContextCount);
    }
    
	private void searchMeContextRecursive(MutableXmlElement node) {
		if ("MeContext".equals(node.getTagName())) {
			// MeContext found - print all XmlElement info under this node
			System.out.println("found MeContext... " + node);
			meContextCount ++;
			return;
		}
		
		for(XmlElement child : node.getChildren()) {
			searchMeContextRecursive((MutableXmlElement) child);
		}
	}

    @Test
    public void testModifyManagedElementAndWrite() throws Exception {
        String inputFile = "src/test/java/com/sofu/simple.xml";
        String outputFile = "src/test/java/com/sofu/simple_modify.xml";
        StaxXmlAdapter adapter = new StaxXmlAdapter(inputFile);
        adapter.parseXml(false);
        
        List<MutableXmlElement> elements = adapter.getRootElements();
        
        MutableXmlElement target = findManagedElementWithId(elements, "GW_SoC_EL_SSokChoL-DU21");
        if (target == null) throw new AssertionError("Target ManagedElement not found");
        target.putAttribute("modified", "true");
        
        try (OutputStream out = new FileOutputStream(outputFile)) {
            elements.get(0).write(out, new MemoryBufferInputStream(inputFile));
        }
        System.out.println("Modified XML written to: " + outputFile);
    }

    // Helper: recursively find ManagedElement with given id
    private MutableXmlElement findManagedElementWithId(List<MutableXmlElement> nodes, String idValue) {
        for (MutableXmlElement node : nodes) {
            MutableXmlElement found = findManagedElementWithId(node, idValue);
            if (found != null) return found;
        }
        return null;
    }
    private MutableXmlElement findManagedElementWithId(MutableXmlElement node, String idValue) {
        if ("ManagedElement".equals(node.getTagName()) && idValue.equals(node.getAttributes().get("id"))) {
            return node;
        }
        for (XmlElement child : node.getChildren()) {
            MutableXmlElement found = findManagedElementWithId((MutableXmlElement) child, idValue);
            if (found != null) return found;
        }
        return null;
    }
}