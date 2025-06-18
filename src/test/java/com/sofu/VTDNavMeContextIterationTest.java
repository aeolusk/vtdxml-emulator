package com.sofu;

import com.sofu.vtdemulate.StaxXmlAdapter;
import com.sofu.vtdemulate.VTDNav;
import com.sofu.vtdemulate.XmlElement;
import org.junit.Test;

public class VTDNavMeContextIterationTest {
    @Test
    public void testIterateMeContextSiblingsAndPrintId() throws Exception {
        String filePath = "src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml";
        StaxXmlAdapter adapter = new StaxXmlAdapter(filePath);
        adapter.startParsing();
        VTDNav nav = new VTDNav(adapter);

        // Find the first MeContext element in the document
        boolean found = nav.toElement("MeContext");
        assert found : "No MeContext element found";
        int count = 0;
        do {
            String id = nav.getAttrVal("id");
            System.out.println("MeContext id: " + id);
            count++;
        } while (nav.toElement(VTDNav.NEXT_SIBLING));
        System.out.println("Total MeContext elements: " + count);
        assert count > 0;
    }
} 