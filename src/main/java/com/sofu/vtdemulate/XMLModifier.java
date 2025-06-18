package com.sofu.vtdemulate;

import java.util.List;

public class XMLModifier {
    private final StaxXmlAdapter adapter;

    public XMLModifier(StaxXmlAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Update the attribute of the given node and ensure tagPositions reflects the change.
     */
    public void updateAttribute(MutableXmlElement element, String name, String value) {
        if(element.getAttributes().containsKey(name)) {
            element.putAttribute(name, value);
        }
    }

    /**
     * Delete the given node from the adapter's tagPositions list.
     */
    public void delete(XmlElement element) {
        if(element.getParent() != null) {
            element.getParent().removeChild(element);
        }
    }
} 