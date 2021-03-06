import java.util.Set;
import java.util.HashSet;

public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	private Set<Short> markedForDeletion;
	
	private final static int BLOCK_SIZE = 512;
	private static final int DIRECT = 11;
	private final static int SHORT_SIZE = 2;
	public final static int MAX_FILE_SIZE = (DIRECT + BLOCK_SIZE / SHORT_SIZE) * BLOCK_SIZE; //11 direct, 1 indirect that stores 256 pointers, each pointing to a block of 512 bytes, public because someone might want to implement a feature that needs to know the max file size, and it makes sense to allow that feature to get the number from the file system
	
		public FileSystem(int diskBlocks)
	{
		SysLib.cerr("Initializing FileSystem");

		// create superblock, and format disk with 64 inodes in default
		superblock = new SuperBlock(diskBlocks);

		SysLib.cerr("Superblock created.\n");

		// create directory and register / in directory entry 0
		directory = new Directory(superblock.totalInodes);

		SysLib.cerr("Directory created.\n");

		//file table is created and store directory in the file table
		filetable = new FileTable(directory);

		SysLib.cerr("Filetable created.\n");

		//directory reconstruction
		FileTableEntry dirEnt = open( "/", "r");
		int dirSize = fsize( dirEnt );
		if( dirSize > 0) 
		{
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory( dirData );
		}
		markedForDeletion = new HashSet<Short>();
		close(dirEnt);
	}
	
	//sync the file systems metadata into the disk
	void sync() 
	{
		FileTableEntry ftEntry = open("/", "w");		//read directory from disk
		byte[] buffer = directory.directory2bytes();
		write(ftEntry, buffer);
		close(ftEntry);
		superblock.sync();					//write superblock back to disk
	}
	
	public boolean format( int maxfiles ) 
	{
		//spin lock
		while(!filetable.fempty())
		{
		}
		
		superblock.format(maxfiles);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		return true;
	}
	
	//open file with mentioned mode
	public FileTableEntry open(String filename, String mode)
	{
		FileTableEntry ftEnt = filetable.falloc(filename, mode);
		//checking mode for writing
		if (ftEnt != null && mode.equals("w"))
			if(!deallocAllBlocks(ftEnt))
				ftEnt = null;
		return ftEnt;
	}
	
	private boolean deallocAllBlocks(FileTableEntry entry) {
		return changeSize(entry.inode, entry.inode.length, 0);
	}

	// Closes the file and free the input from the table
	// decrement the entry count while other threads still hold access to this entry
	public boolean close(FileTableEntry ftEnt)
	{
		 synchronized(ftEnt) {
			 ftEnt.count--;			 
			 if (markedForDeletion.contains(ftEnt.iNumber) && ftEnt.count == 0) {
		 		deallocate(ftEnt.inode, ftEnt.iNumber);
		 	 }
			 if(ftEnt.count > 0) {
				 return true;
			 }
		 }
		 return filetable.ffree(ftEnt);
	}
	
	public int fsize( FileTableEntry ftEnt)
	{
		synchronized (ftEnt)
		{
			return ftEnt.inode.length;
		}
	}
	
	public int read (FileTableEntry ftEnt, byte[] buffer )
	{
		int seekPtr = ftEnt.seekPtr;
		int blockIndex = seekPtr / BLOCK_SIZE;
		byte[] indirectBlock = null;
		if (seekPtr + buffer.length > DIRECT * BLOCK_SIZE) {
			indirectBlock = new byte[BLOCK_SIZE];
			SysLib.cread(ftEnt.inode.indirect, indirectBlock);
		}
		int blockNumber = getBlockNumber(ftEnt.inode, blockIndex, indirectBlock);
		byte[] contentBlock = new byte[BLOCK_SIZE];
		SysLib.cread(blockNumber, contentBlock);
		int lengthRead = buffer.length;
		if (ftEnt.inode.length - seekPtr < lengthRead) {
			lengthRead = ftEnt.inode.length - seekPtr;
		}
		for (int i = 0; i < lengthRead; i++) {
			buffer[i] = contentBlock[(seekPtr + i) % BLOCK_SIZE];
			if ((seekPtr + i) % BLOCK_SIZE == 0 && i != buffer.length - 1) {
				blockIndex++;
				blockNumber = getBlockNumber(ftEnt.inode, blockIndex, indirectBlock);
				SysLib.cread(blockNumber, contentBlock);
			}
		}
		ftEnt.seekPtr += lengthRead;
		return lengthRead;
	}

	public int write(FileTableEntry ftEnt, byte[] buffer )
	{
		int seekPtr = ftEnt.seekPtr;
		int blockIndex = seekPtr / BLOCK_SIZE;
		byte[] indirectBlock = null;
		if (seekPtr + buffer.length > DIRECT * BLOCK_SIZE) {
			indirectBlock = new byte[BLOCK_SIZE];
			SysLib.cread(ftEnt.inode.indirect, indirectBlock);
		}
		
		//SysLib.cerr("\ncurrent block number for write: " + Integer.toString(blockNumber));
		byte[] contentBlock = new byte[BLOCK_SIZE];
		int lengthWritten = buffer.length;
		synchronized (ftEnt) {
			if (seekPtr + lengthWritten > ftEnt.inode.length) {
				SysLib.cerr("Inside the change size if statement");
				if (!changeSize(ftEnt.inode, ftEnt.inode.length, seekPtr + lengthWritten)) {
					return -1; // not enough space on disk
				}
			}
			int blockNumber = getBlockNumber(ftEnt.inode, blockIndex, indirectBlock);
			if (seekPtr % BLOCK_SIZE != 0) {
				SysLib.cread(blockNumber, contentBlock);
			}
			if (MAX_FILE_SIZE - seekPtr < lengthWritten) {
				lengthWritten = MAX_FILE_SIZE - seekPtr;
			}
			SysLib.cerr("\nWriting " + Integer.toString(lengthWritten) + " bytes");
			for (int i = 0; i < lengthWritten; i++) {
				contentBlock[(seekPtr + i) % BLOCK_SIZE] = buffer[i];
				if ((seekPtr + i) % BLOCK_SIZE == BLOCK_SIZE - 1 && i != buffer.length - 1) {
					SysLib.cerr("\nChanging blocks at i=" + Integer.toString(i));
					SysLib.cerr("\ncurrent block number for write: " + Integer.toString(blockNumber));
					SysLib.cwrite(blockNumber, contentBlock);
					blockIndex++;
					blockNumber = getBlockNumber(ftEnt.inode, blockIndex, indirectBlock);
					if (lengthWritten - i < BLOCK_SIZE) { // if the loop will end before writing the whole block
						SysLib.cerr("\ncurrent block number for write: " + Integer.toString(blockNumber));
						SysLib.cread(blockNumber, contentBlock);
					}
				}
			}
			SysLib.cwrite(blockNumber, contentBlock);
		}
		ftEnt.seekPtr += lengthWritten;
		return lengthWritten;
	}

	public boolean delete ( String filename )
	{
		short inumber = directory.namei(filename);
		Inode inode = new Inode(inumber);
		boolean exists = (inode.flag != 0);
		if (exists) {
			if (inode.count == 0) {
				deallocate(inode, inumber);
			}
			else {
				if (markedForDeletion.contains(inumber)) {
					return false;
				}
				else {
					markedForDeletion.add(inumber);
				}
			}
		}
		return exists;
	}
	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	public int seek (FileTableEntry ftEnt, int offset, int whence)
	{
		int tempSeek = ftEnt.seekPtr;
		if (whence == SEEK_SET) {
			tempSeek = offset;
		}
		else if (whence == SEEK_CUR) {
			tempSeek += offset;
		}
		else if (whence == SEEK_END) {
			tempSeek = ftEnt.inode.length + offset;
		}
		else {
			throw new IllegalArgumentException();
		}
		if (tempSeek < 0 || tempSeek > ftEnt.inode.length) {
			throw new IllegalArgumentException();
		}
		ftEnt.seekPtr = tempSeek;
		return ftEnt.seekPtr;
	}
	
	private void deallocate(Inode inode, short inumber) {
		changeSize(inode, inode.length, 0);
		inode.flag = 0;
		// Clearing the flag after changing the size (as opposed to before), should prevent a race condition. I think.
		inode.toDisk(inumber);
	}
	
	// Resizes a file. Returns true if successful.
	private boolean changeSize(Inode inode, int bytesBefore, int bytesAfter) {
		SysLib.cerr("\nChanging size from " + Integer.toString(bytesBefore) + " to " + Integer.toString(bytesAfter));
		SysLib.cerr("\nDirect pointers before:");
		for (int i = 0; i < DIRECT; i++) {
			SysLib.cerr(" " + Integer.toString(inode.direct[i]));
		}
		if (bytesAfter > MAX_FILE_SIZE) {
			return false;
		}
		int blocksBefore = neededBlocks(bytesBefore);
		int blocksAfter = neededBlocks(bytesAfter);
		SysLib.cerr("\nChanging number of blocks from " + Integer.toString(blocksBefore) + " to " + Integer.toString(blocksAfter));
		if (blocksBefore == blocksAfter) {
			return true;
		}
		byte[] buffer = null;
		if (blocksBefore > DIRECT || blocksAfter > DIRECT) {
			// Only do a read if we care about the indirect block
			buffer = new byte[BLOCK_SIZE];
			SysLib.cread(inode.indirect, buffer);
		}
		if (blocksBefore > blocksAfter) {
			SysLib.cerr("\nDecreasing size...");
			for (int i = blocksAfter; i < blocksBefore; i++) {
				deallocateBlock(inode, i, buffer);
			}
			if (blocksAfter > DIRECT) {
				SysLib.cwrite(inode.indirect, buffer);
			}
		}
		else {
			assert (blocksAfter > blocksBefore);
			SysLib.cerr("\nIncreasing size...");
			for (int i = blocksBefore; i < blocksAfter; i++) {
				allocateBlock(inode, i, buffer);
			}
		}
		SysLib.cerr("\nDirect pointers after:");
		for (int i = 0; i < DIRECT; i++) {
			SysLib.cerr(" " + Integer.toString(inode.direct[i]));
		}
		inode.length = bytesAfter;
		return true;
	}
	
	private void deallocateBlock(Inode inode, int blockIndex, byte[] indirectBlock) {
		superblock.returnBlock(getBlockNumber(inode, blockIndex, indirectBlock));
	}
	
	private void allocateBlock(Inode inode, int blockIndex, byte[] indirectBlock) {
		short blockNumber = (short)superblock.getBlock();
		SysLib.cerr("\nPointing index " + Integer.toString(blockIndex) + " to block " + Short.toString(blockNumber));
		setBlockNumber(blockNumber, inode, blockIndex, indirectBlock);
	}
	
	private static short getBlockNumber(Inode inode, int blockIndex, byte[] indirectBlock) {
		SysLib.cerr("\nblockIndex: " + Integer.toString(blockIndex));
		if (blockIndex < DIRECT) {
			SysLib.cerr(", blockNumber: " + Integer.toString(inode.direct[blockIndex]));
			return inode.direct[blockIndex];
		}
		else {
			int offset = (blockIndex - DIRECT) * SHORT_SIZE;
			SysLib.cerr(", blockNumber: " + Integer.toString(SysLib.bytes2short(indirectBlock, offset)));
			return SysLib.bytes2short(indirectBlock, offset);
		}
	}
	
	private static void setBlockNumber(short blockNumber, Inode inode, int blockIndex, byte[] indirectBlock) {
		if (blockIndex < DIRECT) {
			inode.direct[blockIndex] = blockNumber;
		}
		else {
			int offset = (blockIndex - DIRECT) * SHORT_SIZE;
			SysLib.short2bytes(blockNumber, indirectBlock, offset);
		}
	}
	
	// Gets the number of blocks needed to store a file of given number of bytes
	private static int neededBlocks(int bytes) {
		int ret = bytes / BLOCK_SIZE;
		if (bytes % BLOCK_SIZE != 0) {
			ret++;
		}
		return ret;
	}
}
