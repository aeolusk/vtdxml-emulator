package com.sofu.vtdemulate;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class VTDNav {
	private StaxXmlAdapter adapter;
	private XmlElement currentTagPosition; // Current position in the XML tree
	private Stack<XmlElement> positionStack; // Stack for push/pop operations

	public static final int PARENT = 0;
	public static final int FIRST_CHILD = 1;
	public static final int NEXT_SIBLING = 2;

	private static Set<String> topNodes = new HashSet<>();

	static {
		topNodes.add("bulkCmConfigDataFile");
		topNodes.add("configData");
		topNodes.add("SubNetwork");
	}

	public VTDNav() {
		this.positionStack = new Stack<>();
	}

	public VTDNav(StaxXmlAdapter adapter) {
		this(); // Call default constructor to initialize stack
		this.adapter = adapter;
		// Wait up to 1 second for adapter.getTagPositions() to become non-null
		if (adapter != null) {
			waitForTagPositionsNotNull(adapter, 2000);
			// Initialize currentTagPosition to the first root node if available
			if (adapter.getFirstRootElement() != null) {
				this.currentTagPosition = adapter.getFirstRootElement();
				System.out.println("currentTagPosition=" + this.currentTagPosition);
			}
		}
	}

	/**
	 * Waits until adapter.getTagPositions() is not null or timeout is reached.
	 * 
	 * @param adapter       The StaxXmlAdapter instance
	 * @param timeoutMillis Maximum wait time in milliseconds
	 */
	private static void waitForTagPositionsNotNull(StaxXmlAdapter adapter, long timeoutMillis) {
		long start = System.currentTimeMillis();
		while (adapter.getFirstRootElement() == null && System.currentTimeMillis() - start < timeoutMillis) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		// System.out.println("waitForTagPositionsNotNull... find " +
		// adapter.getFirstRootElement());
	}

	/**
	 * Saves the current navigation position onto a stack.
	 */
	public void push() {
		if (currentTagPosition != null) {
			positionStack.push(currentTagPosition);
		}
	}

	/**
	 * Restores the last saved navigation position from the stack.
	 */
	public void pop() {
		if (!positionStack.isEmpty()) {
			currentTagPosition = positionStack.pop();
		} else {
			System.err.println("Warning: Position stack is empty. Cannot pop.");
		}
	}

	public XmlElement getCurrTagPosition() {
		return currentTagPosition;
	}

	/**
	 * Returns the string representation of the current element. This attempts to
	 * get the full XML fragment of the current tag.
	 * 
	 * @return The XML fragment of the current element, or null if no element is
	 *         selected.
	 */
	public String toString() {
		return getElementFragment();
	}

	/**
	 * Returns the text content of the current element. (Emulation placeholder)
	 * 
	 * @return The text content of the current element, or an empty string if not
	 *         applicable.
	 */
	public String getText() {
		// A full implementation would parse the text content between start and end tags
		// For now, it's a placeholder
		if (currentTagPosition != null) {
			return "Text content of " + currentTagPosition.getTagName();
		}
		return "";
	}

	/**
	 * Returns the index of the current element. (Emulation placeholder) Note: A
	 * true VTD-XML index is more complex. This is a simple representation.
	 * 
	 * @return A simplified index (e.g., hash code of the TagPosition).
	 */
	public int getCurrentIndex() {
		if (currentTagPosition != null) {
			return currentTagPosition.hashCode(); // Simple placeholder for an index
		}
		return -1;
	}

	/**
	 * Navigates to the next element with the given name, starting from the current
	 * position (like VTD-XML). It searches the subtree (DFS), then siblings, then
	 * parent's siblings, etc.
	 * 
	 * @param elementName The name of the element to navigate to.
	 * @return true if navigation is successful, false otherwise.
	 */
	public boolean toElement(String elementName) {
		if (currentTagPosition == null)
			return false;
		XmlElement found = findNextElementDFS(currentTagPosition, elementName);
		if (found != null) {
			currentTagPosition = found;
			// Wait for full parsing if endOffset is not set
			waitForElementFullyParsed(currentTagPosition, false);
			return true;
		}
		return false;
	}

	/**
	 * DFS: Search all children (subtree) of the current node first, and if not
	 * found, search only its next siblings. Does not go up to the parent for
	 * further search. (Same as VTD-XML)
	 */
	private XmlElement findNextElementDFS(XmlElement start, String elementName) {
		// 1. Search in all children (subtree) of the current node
		for (XmlElement child : start.getChildren()) {
			if (elementName.equals(child.getTagName())) {
				return child;
			}
			XmlElement foundInChild = findNextElementDFS(child, elementName);
			if (foundInChild != null)
				return foundInChild;
		}
		// 2. Search in its next siblings
		XmlElement sibling = start.getNextSibling();
		while (sibling != null) {
			if (elementName.equals(sibling.getTagName())) {
				return sibling;
			}
			XmlElement foundInSibling = findNextElementDFS(sibling, elementName);
			if (foundInSibling != null)
				return foundInSibling;
			sibling = sibling.getNextSibling();
		}
		// 3. Do not search further (do not go up to parent)
		return null;
	}

	/**
	 * Navigates to a related element based on direction (PARENT, FIRST_CHILD,
	 * NEXT_SIBLING).
	 * 
	 * @param direction The direction to navigate (VTDNav.PARENT,
	 *                  VTDNav.FIRST_CHILD, VTDNav.NEXT_SIBLING).
	 * @return true if navigation is successful, false otherwise.
	 */
	public boolean toElement(int direction) {
		return toElement(direction, true);
	}
	
	private boolean toElement(int direction, boolean tryToLoadNextSibling) {
		if (currentTagPosition == null) {
			return false;
		}
		boolean moved = false;
		boolean tryToFindNextSibling = false;

		switch (direction) {
		case PARENT:
			if (currentTagPosition.getParent() != null) {
				currentTagPosition = currentTagPosition.getParent();
				moved = true;
			}
			break;
		case FIRST_CHILD:
			if (!currentTagPosition.getChildren().isEmpty()) {
				currentTagPosition = currentTagPosition.getChildren().get(0);
				moved = true;
			}
			break;
		case NEXT_SIBLING:
			if (currentTagPosition.getNextSibling() != null) {
				currentTagPosition = currentTagPosition.getNextSibling();
				moved = true;
			} else if (tryToLoadNextSibling && "MeContext".equals(currentTagPosition.getTagName())) {
				int blkIdx = adapter.getBlockIndex(currentTagPosition.getEndOffset());
				int loadedBlockEndIdx = adapter.getLoadedBlockEndIndex();

				if (blkIdx == loadedBlockEndIdx) {
					// This block is last block in loaded block. Some sibling isn't loaded at this
					// time.
					tryToFindNextSibling = true;
					System.out.println("This block is last block in loaded block. offset=" + loadedBlockEndIdx);
				}
			}
			break;
		}

		if (moved && !topNodes.contains(currentTagPosition.getTagName())) {
			// Wait for full parsing if endOffset is not set
			waitForElementFullyParsed(currentTagPosition, false);
		} else if(tryToFindNextSibling) {
			waitForElementFullyParsed(currentTagPosition, tryToFindNextSibling);
			return toElement(direction, false);
		}

		return moved;
	}

	/**
	 * Waits until the given element's endOffset is set (>0), triggering/resuming
	 * parsing if needed. Times out after 10 seconds to avoid infinite wait.
	 */
	private void waitForElementFullyParsed(XmlElement element, boolean forceSearch) {
		if (element == null || adapter == null || !adapter.isRunning())
			return;

		if (forceSearch) {
			System.out.println("Find next element if exists... " + adapter.isRunning());
			adapter.resumeParsingAndWait(2000);
			return;
		}

		long start = System.currentTimeMillis();
		long timeout = 10000; // 10 seconds
		// Wait until endOffset is set or timeout
		while (element.getEndOffset() <= 0) {
			System.out.println("Found incompleted element... " + element);
			adapter.resumeParsingAndWait(2000);

			if (System.currentTimeMillis() - start > timeout) {
				System.err.println("Timeout waiting for element to be fully parsed.");
				break;
			}
		}
		System.out.println("After waiting... " + element);
	}

	/**
	 * Returns the value of a specified attribute for the current element (VTD-XML
	 * style).
	 * 
	 * @param attributeName The name of the attribute.
	 * @return The attribute value, or null if not found or no element is selected.
	 */
	public String getAttrVal(String attributeName) {
		if (currentTagPosition == null || attributeName == null)
			return null;
		return currentTagPosition.getAttributes().get(attributeName);
	}

	/**
	 * Returns the XML fragment of the current element.
	 * 
	 * @return The XML fragment as a String, or null if no element is selected.
	 */
	public String getElementFragment() {
		return null;
	}

	/**
	 * Moves to the next sibling element. (Emulation for VTD-XML's next())
	 * 
	 * @return true if moved to next sibling, false otherwise.
	 */
	public boolean next() {
		return toElement(NEXT_SIBLING);
	}

	// public String xpath() - This would be handled by AutoPilot

	public void setCurrentTagPosition(XmlElement tagPosition) {
		this.currentTagPosition = tagPosition;
	}

	public void setAdapter(StaxXmlAdapter adapter) {
		this.adapter = adapter;
	}
}