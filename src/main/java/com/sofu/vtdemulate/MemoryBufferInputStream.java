package com.sofu.vtdemulate;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Buffer class to store XML file in memory and load additional data as needed
 * (extends ByteArrayInputStream)
 */
public class MemoryBufferInputStream extends InputStream {
	public static final int COUNT_OF_CHUNK_IN_BLOCK = 10;
	// private static final int SAVED_BLOCK_SIZE = COUNT_OF_CHUNK_IN_BLOCK * 1024 *
	// 1024; // 30MB
	private static final int READ_CHUNK_SIZE = 3 * 1024 * 1024; // 2MB
	private RandomAccessFile raf;
	private long fileSize;
	private boolean eof = false;
	private int chunkIndex = -1; // Current block index being read
	private long totalRead = 0; // Total bytes read
	private byte[] buf;
	private int pos = 0;
	private int count = 0;
	private int loadedChunkStart = -1;
	private int loadedChunkEnd = -1;
	private boolean blockListNeedsClear = false;

	private static class BlockInfo {
		int chunkOffset;
		byte[] data;

		BlockInfo(int chunkOffset, byte[] data) {
			this.chunkOffset = chunkOffset;
			this.data = data;
		}
	}

	private LinkedList<BlockInfo> blockList = new LinkedList<>();

	public MemoryBufferInputStream(String filePath) throws IOException {
		this.buf = new byte[READ_CHUNK_SIZE];
		this.raf = new RandomAccessFile(filePath, "r");
		this.fileSize = raf.length();
		this.count = 0;
		this.pos = 0;
		this.eof = false;
		this.chunkIndex = -1;
		this.totalRead = 0;
	}

	/**
	 * Constructor for byte array input.
	 * 
	 * @param input The byte array containing the data.
	 */
	public MemoryBufferInputStream(byte[] input) {
		this.buf = input;
		this.fileSize = input.length;
		this.raf = null;
		this.eof = true;
		this.chunkIndex = 0;
		this.totalRead = input.length;
	}

	/**
	 * Loads 30 consecutive blocks starting from block n into blockList. After
	 * read() is called, blockList is cleared and reading starts anew.
	 */
	public synchronized void loadBlocksFrom(int n) throws IOException {
		if (raf == null)
			throw new IOException("Not a file-backed stream");
		int newStart = n;
		int newEnd = n + COUNT_OF_CHUNK_IN_BLOCK - 1;

		// 1. Remove all blocks from blockList except those in the range n~n+29
		Iterator<BlockInfo> it = blockList.iterator();
		while (it.hasNext()) {
			BlockInfo bi = it.next();
			if (bi.chunkOffset < newStart || bi.chunkOffset > newEnd) {
				it.remove();
			}
		}

		// 2. Determine the range of blocks to newly load compared to already loaded
		// blocks
		int toLoadChunkStart = (loadedChunkEnd < newStart || loadedChunkEnd < 0) ? newStart : loadedChunkEnd + 1;
		int toLoadChunkEnd = newEnd;

		// 3. Read only the new blocks from the file and add them
		for (int i = toLoadChunkStart; i <= toLoadChunkEnd; i++) {
			long startPos = (long) i * READ_CHUNK_SIZE;
			if (startPos >= fileSize)
				break;
			raf.seek(startPos);
			byte[] newBuf = new byte[READ_CHUNK_SIZE];
			int readLen = raf.read(newBuf, 0, READ_CHUNK_SIZE);
			if (readLen == -1)
				break;
			if (readLen < READ_CHUNK_SIZE) {
				byte[] lastBuf = new byte[readLen];
				System.arraycopy(newBuf, 0, lastBuf, 0, readLen);
				blockList.add(new BlockInfo(i, lastBuf));
				break;
			}
			blockList.add(new BlockInfo(i, newBuf));
		}

		loadedChunkStart = newStart;
		loadedChunkEnd = newEnd;

		this.buf = blockList.isEmpty() ? new byte[READ_CHUNK_SIZE] : blockList.getFirst().data;
		this.pos = 0;
		this.count = this.buf.length;
		this.eof = false;
		// this.hugeBlockIndex = n / COUNT_OF_CHUNK_IN_BLOCK;
		// this.totalRead = (long) n * BLOCK_SIZE;
		blockListNeedsClear = true;
	}

	@Override
	public synchronized int read() throws IOException {
		if (blockListNeedsClear && !blockList.isEmpty()) {
			blockList.clear();
			blockListNeedsClear = false;
			loadedChunkStart = -1;
			loadedChunkEnd = -1;
		}
		if (eof)
			return -1;
		if (pos >= count) {
			int readLen = 0;
			if (raf != null) {
				buf = new byte[READ_CHUNK_SIZE];
				readLen = raf.read(buf, 0, READ_CHUNK_SIZE);
			} else {
				return -1;
			}
			if (readLen == -1) {
				eof = true;
				return -1;
			}
			this.count = readLen;
			this.pos = 0;
			totalRead += readLen;
			// hugeBlockIndex = (int) (totalRead / BLOCK_SIZE);
			chunkIndex = (int) (totalRead / READ_CHUNK_SIZE);
			blockList.add(new BlockInfo(chunkIndex, buf));
			if (blockList.size() > COUNT_OF_CHUNK_IN_BLOCK) {
				blockList.removeFirst();
			}
			// loadedBlockStart/End 媛깆떊
			if (!blockList.isEmpty()) {
				loadedChunkStart = blockList.getFirst().chunkOffset;
				loadedChunkEnd = blockList.getLast().chunkOffset;
			} else {
				loadedChunkStart = -1;
				loadedChunkEnd = -1;
			}
			System.out.println(">>>>>> read Chunk (size=" + readLen + ", loaded block=" + loadedChunkStart + "~"
					+ loadedChunkEnd + ")");
		}
		return buf[pos++] & 0xFF;
	}

	public long size() {
		return fileSize;
	}

	public boolean isEof() {
		return eof;
	}

	/**
	 * Returns the current block index (in 30MB units).
	 */
	public int getChunkIndex() {
		return chunkIndex;
	}

	public void close() throws IOException {
		if (raf != null)
			raf.close();
	}

	/**
	 * Returns a byte array from startOffset to endOffset (inclusive). For example,
	 * getByte(0, 100) returns bytes 0~100 (101 bytes).
	 */
	public synchronized byte[] getByte(long startOffset, long _endOffset) throws IOException {
		long endOffset = Math.min(_endOffset, fileSize - 1);

		if (startOffset < 0 || endOffset < startOffset) {
			throw new IOException("Invalid offset range");
		}
		int startChunkIndex = (int) (startOffset / READ_CHUNK_SIZE);
		int endChunkIndex = (int) (endOffset / READ_CHUNK_SIZE);

		// Check if all required blocks are present in blockList (using
		// loadedBlockStart/End range)
		boolean allBlocksPresent = (startChunkIndex >= loadedChunkStart && endChunkIndex <= loadedChunkEnd);
		// If not, load them using loadBlocksFrom
		if (!allBlocksPresent) {
			loadBlocksFrom(startChunkIndex);
		}

		// Combine only the required parts from blockList
		int totalLen = (int) (endOffset - startOffset + 1);
		byte[] result = new byte[totalLen];
		int copied = 0;
		long curOffset = startOffset;
		for (int i = startChunkIndex; i <= endChunkIndex; i++) {
			BlockInfo bi = null;
			for (BlockInfo b : blockList) {
				if (b.chunkOffset == i) {
					bi = b;
					break;
				}
			}
			if (bi == null)
				throw new IOException("Block not loaded: " + i);
			int blockStart = (int) (i * READ_CHUNK_SIZE);
			int from = (int) Math.max(curOffset, blockStart);
			int to = (int) Math.min(endOffset, blockStart + bi.data.length);
			int len = to - from + 1;
			System.arraycopy(bi.data, from - blockStart, result, copied, len);
			copied += len;
			curOffset += len;
		}
		return result;
	}

	public int getChunkIndex(long offset) {
		return (int) (offset / READ_CHUNK_SIZE);
	}

	public int getLoadedBlockEndIndex() {
		return loadedChunkEnd;
	}

	/**
	 * Returns the offset of the first '>' character at or after fromOffset, or -1
	 * if not found. Searches loaded blocks first, loads blocks if necessary.
	 */
	public long findPrevGt(long fromOffset) throws IOException {
		long cur = fromOffset;
		int blockSize = READ_CHUNK_SIZE;
		while (cur >= 0) {
			int blockNum = (int) (cur / blockSize);
			BlockInfo foundBlock = null;
			for (BlockInfo b : blockList) {
				if (b.chunkOffset == blockNum) {
					foundBlock = b;
					break;
				}
			}
			if (foundBlock == null) {
				// Block not loaded, load from this block
				return -1; // Should not happen
			}
			int blockOffset = (int) (cur % blockSize);
			for (int i = blockOffset; i >= 0; i--) {
				if (foundBlock.data[i] == '<') {
					return (long) blockNum * blockSize + i;
				}
			}
			cur = ((long) blockNum - 1) * blockSize;
		}
		return -1;
	}
}