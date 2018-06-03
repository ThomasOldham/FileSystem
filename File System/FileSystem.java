

public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	private final int MAXFILESIZE = (267 * 512) //11 direct, 1 indirect that stores 256 pointers, each pointing to a block of 512 bytes
	
	public FileSystem(int diskBlocks)
	{
		// create superblock, and format disk with 64 inodes in default
		superblock = new SuperBlock(diskBlocks);
		
		// create directory and register / in directory entry 0
		directory = new Directory(superblock.totalInodes);
		
		//file table is created and store directory in the file table
		filetable = new FileTable(directory);
		
		//directory reconstruction
		FileTableEntry dirEnt = open( "/", "r");
		int dirSize = fsize( dirEnt );
		if( dirSize > 0) 
		{
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory( dirData );
		}
		close(dirEnt);
	}
	
	void sync() 
	{
	}
	
	boolean format( int maxfiles ) 
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
	FileTableEntry open(String filename, String mode)
	{
		FileTableEntry input = filetable.falloc(filename, mode);
		//checking mode for writeing
		if (input != null && mode.equals("w"))
			if(!deallocAllBlocks(input))
				input = null;
		return input;
	}

	// Closes the file and free the input from the table
	// decrement the entry count while other threads still hold access to this entry
	boolean close(FileTableEntry entry)
	{
		 synchronized(entry)
		 {
			 entry.count--;
			 if(entry.count > 0)
				 return true;
		 }
		 return filetable.ffree(entry);
	}
	
	int fsize( FileTableEntry ftEnt)
	{
		return 1;
	}
	
	int read (FileTableEntry ftEnt, byte[] buffer )
	{
		return 1;
	}

	int write(FileTableEntry ftEnt, byte[] buffer )
	{
		if(freelist == -1 || (buffer.length > (Disk.blockSize * 1000))	//no blocks free or file is too large
			return -1;
		
		synchronized(ftEnt)
		{
			int filesize = fsize(ftEnt);
			int remainingWrite = buffer.length;
			while(remainingWrite > 0)
			{
				int blockID = ftEnt;
			}
		}

	}

	private boolean deallocAllBlocks(FileTableEntry ftEnt)
	{
		return true;
	}

	boolean delete ( String filename )
	{
		return true;
	}
	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	int seek (FileTableEntry ftEnt, int offset, int whence)
	{
		return 1;
	}
}
