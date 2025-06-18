package com.sofu;

import com.sofu.vtdemulate.VTDGen;
import com.sofu.vtdemulate.VTDNav;
import org.junit.Test;

public class VTDNavMeContextSiblingTest {
	@Test
	public void testMeContextRecursiveSearch() {
		// String filePath = "src/test/java/com/sofu/simple.xml";
		String filePath = "src/test/java/com/sofu/export_3GPP_CM_20220916_084116.xml";
		VTDGen gen = new VTDGen();
		boolean parsed = gen.parseFile(filePath);
		assert parsed : "XML parsing failed";

		VTDNav nav = gen.getNav();

		while (true) {
			// Start recursive search from the top node
			searchFirstMeContextRecursive(nav);
			do {
				printMeContext("", nav.getCurrTagPosition());
			} while (nav.toElement(VTDNav.NEXT_SIBLING));

			if (!moveToNextSubNetwork(nav))
				break;
		}
	}

	private boolean moveToNextSubNetwork(VTDNav nav) {
		nav.toElement(VTDNav.PARENT);
		return nav.toElement(VTDNav.NEXT_SIBLING);
	}

	/**
	 * Recursively searches for MeContext nodes from the current position. If
	 * MeContext is found, the logic is left empty as requested. Otherwise,
	 * recursively searches all children and siblings.
	 */
	private boolean searchFirstMeContextRecursive(VTDNav nav) {
		System.out.println("searching... " + nav.getCurrTagPosition());
		// Check current node
		if ("MeContext".equals(nav.getCurrTagPosition().getTagName())) {
			// MeContext found - print all XmlElement info under this node
			System.out.println("found MeContext... " + nav.getCurrTagPosition());
			return true;
		}
		// Search children recursively
		do {
			if (nav.toElement(VTDNav.FIRST_CHILD)) {
				if (searchFirstMeContextRecursive(nav))
					return true;
			}
		} while (nav.toElement(VTDNav.NEXT_SIBLING));
		nav.toElement(VTDNav.PARENT); // Return to parent after finishing children
		return false;
	}

	/**
	 * Recursively prints all XmlElement info under the given node (including
	 * itself).
	 */
	private void printMeContext(String tab, com.sofu.vtdemulate.XmlElement node) {
		System.out.println(tab + node);
	}
}