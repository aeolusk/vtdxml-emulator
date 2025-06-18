package com.sofu.vtdemulate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.stax.WstxInputFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StaxXmlAdapter implements Runnable {
	private String filePath;

	private List<MutableXmlElement> rootNodes = new CopyOnWriteArrayList<>(); // Top-level nodes (multiple possible)
	private MemoryBufferInputStream memoryBuffer;

	// Threading and pause/resume control
	private Thread parserThread;
	private volatile boolean running = false;
	private volatile AtomicBoolean paused = new AtomicBoolean(false);
	private final Lock pauseLock = new ReentrantLock();
	private final Condition pauseCondition = pauseLock.newCondition();

	/**
	 * Constructor for file-based XML parsing. Does not start parsing automatically.
	 * 
	 * @param filePath The path to the XML file.
	 */
	public StaxXmlAdapter(String filePath) {
		this.filePath = filePath;
		try {
			this.memoryBuffer = new MemoryBufferInputStream(filePath);
		} catch (Exception e) {
			log.error("{}", e);
			this.memoryBuffer = null;
		}
	}

	public String getFilePath() {
		return filePath;
	}

	/**
	 * Constructor for stream-based XML parsing. Does not start parsing
	 * automatically.
	 * 
	 * @param mbis The MemoryBufferInputStream containing the XML data.
	 */
	public StaxXmlAdapter(MemoryBufferInputStream mbis) {
		this.memoryBuffer = mbis;
	}

	/**
	 * Starts parsing in a separate thread.
	 */
	public boolean startParsing() {
		if (parserThread == null || !parserThread.isAlive()) {
			running = true;
			paused.set(false);
			parserThread = new Thread(this, "StaxXmlAdapter-ParserThread");
			parserThread.start();

			return waitForNextBlockLoaded(-1, 10000);
		}
		return false;
	}

	/**
	 * Resumes parsing if paused.
	 */
	private AtomicInteger currentChunkIndex = new AtomicInteger(0);

	public void resumeParsingAndWait(int maxWailMillies) {
		int chunkIndex = 0;
		pauseLock.lock();
		try {
			chunkIndex = currentChunkIndex.get();
			System.out.println("resumeParsing... chunkIndex=" + chunkIndex);
			paused.set(false);
			pauseCondition.signalAll();

		} finally {
			pauseLock.unlock();
		}

		long start = System.currentTimeMillis();
		// Wait until endOffset is set or timeout
		while (chunkIndex == currentChunkIndex.get() && !isTerminated.get()) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (System.currentTimeMillis() - start > maxWailMillies) {
				System.err.println("Timeout waiting for element to be fully parsed.");
				break;
			}
		}

	}

	/**
	 * Stops parsing and terminates the thread.
	 */
	public void stopParsing() {
		resumeParsingAndWait(2000); // Wake up if paused
	}

	/**
	 * Returns true if parsing is currently paused.
	 */
	public boolean isPaused() {
		return paused.get();
	}

	/**
	 * Returns true if parsing is running.
	 */
	public boolean isRunning() {
		return running;
	}

	@Override
	public void run() {
		try {
			parseXml(true);
		} catch (Exception e) {
			log.error("Parsing error: {}", e);
		} finally {
			isLoading.set(false);
			isTerminated.set(true);
			running = false;
		}
	}

	private Map<Integer, List<MutableXmlElement>> meContextByBlock = new HashMap<>();

	private void saveMeContextInfoForMemoryManagement(MutableXmlElement tag) {
		if ("MeContext".equals(tag.getTagName())) {
			int chunkIndex = memoryBuffer.getChunkIndex(tag.getEndOffset());
			meContextByBlock.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(tag);
			// System.out.println(">> MeContextTag... " + tag);
			if("CN_GJ_EL_SGongAmL_DU01".equals(tag.getAttrubute("id"))) {
				System.out.println(">> MeContextTag... " + tag);
				System.out.println(">> MeContextTag... " + tag.getParent());
			}
		}
	}

	private void removeMeContextsInPrevBlock(int removeBlockIndex) {
		// for memory management.
		List<MutableXmlElement> prevBlockMeContexts = meContextByBlock.get(removeBlockIndex);
		if (prevBlockMeContexts != null) {
			for (MutableXmlElement meCtx : prevBlockMeContexts) {
				MutableXmlElement parent = (MutableXmlElement) meCtx.getParent();
				if (parent != null) {
					parent.getChildren().remove(meCtx);
				}
			}
			meContextByBlock.remove(removeBlockIndex);
		}
	}

	/**
	 * Threaded, pausable XML parsing. Pauses at block boundaries.
	 */
	private AtomicBoolean isLoading = new AtomicBoolean(false);
	private AtomicBoolean isTerminated = new AtomicBoolean(false);

	public void parseXml(boolean vtdNavMode) throws IOException, XMLStreamException {
		InputStream inputStream = memoryBuffer;
		WstxInputFactory factory = new WstxInputFactory();
		factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		XMLStreamReader2 reader = (XMLStreamReader2) factory.createXMLStreamReader(inputStream);
		Stack<MutableXmlElement> stack = new Stack<>();

		rootNodes.clear();
		int startBlockIndex = 0;
		int lastLoadedBlockIndex = 0;

		isLoading.set(true);
		while (reader.hasNext()) {
			int event = reader.next();
			int currentBlockIndex = memoryBuffer.getChunkIndex();
			// Pause at block boundary if requested
			if (currentBlockIndex > MemoryBufferInputStream.COUNT_OF_CHUNK_IN_BLOCK
					&& currentBlockIndex != lastLoadedBlockIndex) {
				if (vtdNavMode) {
					pauseLock.lock();
					System.out.println(">>>>> lock here~ blockIdx=" + currentBlockIndex);
					currentChunkIndex.set(currentBlockIndex);
					isLoading.set(false);
					paused.set(true);
					try {
						while (paused.get()) {
							pauseCondition.await();
						}
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					} finally {
						isLoading.set(true);
						pauseLock.unlock();
						System.out.println(">>>>> start loading next block. blockIdx=" + (currentBlockIndex + 1));
					}
				}
				removeMeContextsInPrevBlock(startBlockIndex);
				startBlockIndex = startBlockIndex + 1;
				lastLoadedBlockIndex = currentBlockIndex;
			}
			switch (event) {
			case XMLStreamConstants.START_ELEMENT: {
				LocationInfo locInf = reader.getLocationInfo();
				long startOffset = locInf.getStartLocation().getCharacterOffset();
				String tagName = reader.getLocalName();
				MutableXmlElement tag = new MutableXmlElement();
				tag.setTagName(tagName);
				tag.setStartOffset(startOffset);
				for (int i = 0; i < reader.getAttributeCount(); i++) {
					String attrName = reader.getAttributeLocalName(i);
					String attrValue = reader.getAttributeValue(i);
					tag.getAttributes().put(attrName, attrValue);
				}
				if (stack.isEmpty()) {
					rootNodes.add(tag);
				} else {
					MutableXmlElement parent = stack.peek();
					parent.addChild(tag);
				}
				stack.push(tag);
				break;
			}
			case XMLStreamConstants.CHARACTERS: {
				if (!stack.isEmpty()) {
					MutableXmlElement current = stack.peek();
					if (!reader.isWhiteSpace()) {
						String text = reader.getText();
						current.getTextNodes().add(text);
					}
				}
				break;
			}
			case XMLStreamConstants.END_ELEMENT: {
				MutableXmlElement tag = stack.pop();
				LocationInfo locInf = reader.getLocationInfo();
				long endOffset = locInf.getEndLocation().getCharacterOffset();
				tag.setEndOffset(endOffset);
				saveMeContextInfoForMemoryManagement(tag);
				handleXmlElement(tag);
				break;
			}
			default:
				break;
			}
		}
		reader.close();
		isLoading.set(false);
		isTerminated.set(true);
		running = false;
	}

	protected void handleXmlElement(MutableXmlElement tag) {
		// TODO: Custom element handling logic
	}

	private void waitForCompleteBlockLoading() {
		while (isLoading.get()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	public List<MutableXmlElement> getRootElements() {
		waitForCompleteBlockLoading();
		return rootNodes;
	}

	public MutableXmlElement getFirstRootElement() {
		waitForCompleteBlockLoading();
		if (rootNodes.size() > 0)
			return rootNodes.get(0);
		return null;
	}

	/**
	 * Waits until the next block is loaded in MemoryBufferInputStream, or timeout
	 * expires.
	 * 
	 * @param timeoutMillis Timeout in milliseconds
	 * @return true if the next block was loaded, false if timeout
	 */
	public boolean waitForNextBlockLoaded(int prevBlockIndex, int timeoutMillis) {
		if (memoryBuffer == null)
			return false;
		waitForCompleteBlockLoading();

		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutMillis) {
			if (memoryBuffer.getChunkIndex() > prevBlockIndex) {
				System.out.println("After read next block " + memoryBuffer.getChunkIndex());
				return true;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return false;
	}

	public int getBlockIndex(long position) {
		return this.memoryBuffer.getChunkIndex(position);
	}

	public int getLoadedBlockEndIndex() {
		return this.memoryBuffer.getLoadedBlockEndIndex();
	}
}