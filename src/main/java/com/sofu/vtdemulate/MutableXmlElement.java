package com.sofu.vtdemulate;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MutableXmlElement extends XmlElement {
	// Change record for attributes
	public static class AttributeChange {
		public enum Type {
			ADD, REMOVE, MODIFY
		}

		public Type type;
		public String key;
		public String newValue;

		public AttributeChange(Type type, String key, String newValue) {
			this.type = type;
			this.key = key;
			this.newValue = newValue;
		}
	}

	// Change record for textNodes
	public static class TextNodeChange {
		public enum Type {
			APPEND, REMOVE, MODIFY
		}

		public Type type;
		public String newValue;

		public TextNodeChange(Type type, String newValue) {
			this.type = type;
			this.newValue = newValue;
		}
	}

	private final List<AttributeChange> attributeChanges = new ArrayList<>();
	private final List<TextNodeChange> textNodeChanges = new ArrayList<>();

	public MutableXmlElement() {
		super();
	}

	// Attribute modification methods
	public void putAttribute(String key, String value) {
		AttributeChange.Type type = AttributeChange.Type.ADD; // Simplified: always ADD for demo
		attributeChanges.add(new AttributeChange(type, key, value));
	}

	public void removeAttribute(String key) {
		attributeChanges.add(new AttributeChange(AttributeChange.Type.REMOVE, key, null));
	}

	// TextNode modification methods
	public void addTextNode(String value) {
		textNodeChanges.add(new TextNodeChange(TextNodeChange.Type.APPEND, value));
	}

	public void setTextNode(int index, String value) {
		List<String> textNodes = getTextNodes();
		textNodes.set(index, value);
		textNodeChanges.add(new TextNodeChange(TextNodeChange.Type.MODIFY, value));
	}

	public void removeTextNode() {
		textNodeChanges.add(new TextNodeChange(TextNodeChange.Type.REMOVE, null));
	}

	public List<AttributeChange> getAttributeChanges() {
		return attributeChanges;
	}

	public List<TextNodeChange> getTextNodeChanges() {
		return textNodeChanges;
	}

	@Override
	protected void writePrefix(OutputStream out, MemoryBufferInputStream memoryBuffer, long startOffset,
			long endOffset) throws IOException {
		boolean attrChanged = !attributeChanges.isEmpty();
		boolean textChanged = !textNodeChanges.isEmpty();
		if (!attrChanged && !textChanged) {
			// No changes, use original
			super.writePrefixTextAndTail(out, memoryBuffer, startOffset, endOffset);
			return;
		}

		byte[] bytes = memoryBuffer.getByte(startOffset, endOffset);
		String xml = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		StringBuilder xmlBuilder = new StringBuilder(xml);

		for (AttributeChange change : attributeChanges) {
			processAttributeChange(xmlBuilder, change);
		}
		out.write(xmlBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return;
	}
	
	@Override
	protected void writePrefixTextAndTail(OutputStream out, MemoryBufferInputStream memoryBuffer, long startOffset,
			long endOffset) throws IOException {
		boolean attrChanged = !attributeChanges.isEmpty();
		boolean textChanged = !textNodeChanges.isEmpty();
		if (!attrChanged && !textChanged) {
			// No changes, use original
			super.writePrefixTextAndTail(out, memoryBuffer, startOffset, endOffset);
			return;
		}

		byte[] bytes = memoryBuffer.getByte(startOffset, endOffset);
		String xml = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
		StringBuilder xmlBuilder = new StringBuilder(xml);

		for (AttributeChange change : attributeChanges) {
			processAttributeChange(xmlBuilder, change);
		}

		int startTextOffset = xmlBuilder.indexOf(">") + 1;
		for (TextNodeChange change : textNodeChanges) {
			if (startTextOffset < 0)
				break;

			int endTextOffset = xmlBuilder.indexOf("</", startTextOffset) - 1;
			if (endTextOffset < 0)
				break;

			processTextNodeChange(xmlBuilder, change, startTextOffset, endTextOffset);
		}

		out.write(xmlBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return;
	}

	/**
	 * Apply attribute changes (add, modify, remove) in-place to the start tag
	 * portion of the StringBuilder. Modifies xmlBuilder directly.
	 */
	protected void processAttributeChange(StringBuilder origXml, AttributeChange change) {
		String key = change.key;
		String newValue = change.newValue;
		AttributeChange.Type type = change.type;
		int tagEnd = origXml.lastIndexOf(">");
		if (tagEnd == -1)
			return; // fallback

		String attrPattern = key + "=";
		int attrPos = origXml.indexOf(attrPattern);
		if (type == AttributeChange.Type.ADD) {
			// Insert new attribute before tagEnd
			String insertStr = " " + key + "=\"" + escapeXml(newValue) + "\"";
			origXml.insert(tagEnd, insertStr);
		} else if (type == AttributeChange.Type.MODIFY) {
			if (attrPos == -1)
				return;
			int valueStart = origXml.indexOf("\"", attrPos) + 1;
			int valueEnd = origXml.indexOf("\"", valueStart);
			origXml.replace(valueStart, valueEnd, escapeXml(newValue));
		} else if (type == AttributeChange.Type.REMOVE) {
			if (attrPos == -1)
				return;
			// Remove from previous space to end of value
			int spaceBefore = origXml.lastIndexOf(" ", attrPos);
			int valueStart = origXml.indexOf("\"", attrPos) + 1;
			int valueEnd = origXml.indexOf("\"", valueStart);
			origXml.delete(spaceBefore, valueEnd + 1);
		}
	}

	/**
	 * Apply text node changes (add, modify, remove) in-place to the text content
	 * portion of the StringBuilder. Modifies xmlBuilder directly.
	 */
	protected void processTextNodeChange(StringBuilder origXml, TextNodeChange change, int startTextOffset,
			int endTextOffset) {
		TextNodeChange.Type type = change.type;
		String newValue = change.newValue;

		if (type == TextNodeChange.Type.APPEND) {
			origXml.insert(endTextOffset, escapeXml(newValue));
		} else if (type == TextNodeChange.Type.MODIFY) {
			origXml.replace(startTextOffset, endTextOffset, escapeXml(newValue));
		} else if (type == TextNodeChange.Type.REMOVE) {
			origXml.delete(startTextOffset, endTextOffset);
		}
	}

	// Simple XML escape for attribute values and text
	private String escapeXml(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&apos;");
	}
}