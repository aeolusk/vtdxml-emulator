# vtd-xml StAX Emulator

## Overview

This project is a Java-based emulator for the VTD-XML parser, using StAX for efficient XML navigation and memory management. It is designed to handle large XML files (hundreds of MB) with minimal memory usage, while providing a VTD-XML-like API for navigation and querying.

## Key Components

- **MemoryBufferInputStream**: Custom InputStream that reads XML files in 1MB chunks and tracks block positions (30MB units) for efficient memory and node management.
- **TagPosition**: Stores tag name, start/end offsets, parent/child relationships, attributes, and text nodes for each XML element.
- **StaxXmlAdapter**: StAX-based parser that builds a tree of TagPosition objects, manages block-based memory cleanup, and provides navigation/querying support.
- **VTDNav**: Provides VTD-XML-like navigation (toElement, getText, getAttrVal, etc.) using TagPosition and StaxXmlAdapter.

## Usage

1. **Parsing an XML file**
   ```java
   VTDGen gen = new VTDGen();
   boolean parsed = gen.parseFile("path/to/your.xml");
   VTDNav nav = gen.getNav();
   ```
2. **Navigating the XML**
   ```java
   nav.toElement("MeContext");
   String text = nav.getText();
   String attr = nav.getAttrVal("id");
   ```
3. **Block-based Memory Management**
   - MeContext nodes are tracked per 30MB block. When a new block is loaded, MeContext nodes from the previous block are removed from the tree to free memory.

## Architecture

- **Efficient Memory Use**: Only tag position and minimal text/attribute data are kept in memory. Large XML content is streamed.
- **StAX Parsing**: Uses javax.xml.stream for fast, forward-only parsing.
- **Custom Navigation**: TagPosition tree enables VTD-XML style navigation without full DOM loading.

## Running Tests

- JUnit tests are provided in `src/test/java/com/sofu/`.
- Example: `VTDGenTest` includes tests for parsing, navigation, and MeContext node counting after memory cleanup.

## Contribution

- Please see TODO.md and sub-tasks for development guidelines and open tasks.