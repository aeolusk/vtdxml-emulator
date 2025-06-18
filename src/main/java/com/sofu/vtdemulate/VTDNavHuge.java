package com.sofu.vtdemulate;

/**
 * Emulation for VTD-XML's VTDNavHuge class.
 * This class extends VTDNav and is primarily a placeholder as current MemoryBufferInputStream
 * already handles large file offsets with long, making a separate "Huge" navigation class less critical
 * in this emulation context.
 */
public class VTDNavHuge extends VTDNav {

    public VTDNavHuge() {
        super();
    }

    public VTDNavHuge(StaxXmlAdapter adapter) {
        super(adapter);
    }

    // All methods from VTDNav are inherited. Specific "Huge" optimizations are not emulated.
} 