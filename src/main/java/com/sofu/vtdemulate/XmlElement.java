package com.sofu.vtdemulate;

import java.io.IOException;
import java.io.OutputStream;
// @ToString(exclude = {"parent", "nextChild"})
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model class to store XML tag position information
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class XmlElement {
	private long startOffset;
	private long endOffset;

	private String tagName;
	private XmlElement parent;
	private List<XmlElement> children = new ArrayList<>();
	/**
	 * Reference to the next sibling child (set by parent when addChild is called)
	 */
	private XmlElement nextSibling;
	private Map<String, String> attributes = new HashMap<>();
	private List<String> textNodes = new ArrayList<>();

	/**
	 * Adds a child to this tag. Sets the previous last child's nextChild to the new
	 * child.
	 * 
	 * @param child The child XmlElement to add.
	 */
	public void addChild(XmlElement child) {
		child.setParent(this);
		if (!this.children.isEmpty()) {
			// Set nextChild of the previous last child to the new child
			XmlElement prev = this.children.get(this.children.size() - 1);
			prev.setNextSibling(child);
		}
		this.children.add(child);
	}

	/**
	 * Removes the given child from this element's children list.
	 */
	public void removeChild(XmlElement child) {
		if (children.remove(child)) {
			child.setParent(null);
		}
	}
	
	public String getAttrubute(String key) {
		return attributes.get(key);
	}

	/**
	 * Recursively writes this element and its children to the output stream.
	 * 
	 * @param out          OutputStream to write to
	 * @param memoryBuffer MemoryBufferInputStream for reading bytes
	 * @throws IOException if an I/O error occurs
	 */
	public void write(OutputStream out, MemoryBufferInputStream memoryBuffer) throws IOException {
		writeInner(out, memoryBuffer);
	}

	/**
	 * Helper for write(). Returns the last written offset.
	 */
	protected long writeInner(OutputStream out, MemoryBufferInputStream memoryBuffer) throws IOException {
		// Write from start to first child
		if (children.isEmpty()) {
			writePrefixTextAndTail(out, memoryBuffer, getStartOffset(), getEndOffset());
			return getEndOffset();
		}
		long lastEndOffset = getStartOffset();
		boolean printPrefix = false;
		for (XmlElement child : children) {
			if (!printPrefix) {
				writePrefix(out, memoryBuffer, getStartOffset(), child.getStartOffset() - 1);
				printPrefix = true;
			} else if(lastEndOffset + 1 < child.getStartOffset()) {
				// After second of children.
				// write space or enter keyword for formatting.
				byte[] bytes = memoryBuffer.getByte(lastEndOffset + 1, child.getStartOffset() - 1);
				out.write(bytes);
			}
			lastEndOffset = child.writeInner(out, memoryBuffer);
		}
		if (lastEndOffset < getEndOffset()) {
			writeTail(out, memoryBuffer, lastEndOffset + 1, getEndOffset());
		}
		return getEndOffset();
	}

	/**
	 * Writes the body of this element when there are no children. Default: writes
	 * original bytes from start to endOffset. Subclasses can override to handle
	 * modified attributes/text.
	 */
	protected void writePrefix(OutputStream out, MemoryBufferInputStream memoryBuffer, long startOffset,
			long endOffset) throws IOException {
		byte[] bytes = memoryBuffer.getByte(startOffset, endOffset);
		out.write(bytes);
	}
	
	protected void writeTail(OutputStream out, MemoryBufferInputStream memoryBuffer, long startOffset,
			long endOffset) throws IOException {
		byte[] bytes = memoryBuffer.getByte(startOffset, endOffset);
		out.write(bytes);
	}
	
	
	protected void writePrefixTextAndTail(OutputStream out, MemoryBufferInputStream memoryBuffer, long startOffset,
			long endOffset) throws IOException {
		byte[] bytes = memoryBuffer.getByte(startOffset, endOffset);
		out.write(bytes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for(Entry<String, String> entry : attributes.entrySet()) {
			if(sb.length() > 0)
				sb.append(",");
			sb.append(entry.getKey() + "=" + entry.getValue());
		}
		return "XmlElement [startOffset=" + startOffset + ", endOffset=" + endOffset + ", tagName=" + tagName
				+ ", attributes=[" + sb.toString() + "], textNodes=" + textNodes + "]";
	}
}