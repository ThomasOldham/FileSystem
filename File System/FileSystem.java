public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	
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
	
	boolean format( int maxFiles ) 
	{
		//spin lock
		while()
		{
		}
		
		superblock.format(maxFiles);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		return true;
	}
	
	//open file with mentioned mode
	FileTableEntry open(String filename, String mode)
	{
		FileTableEntry input = fileTable.falloc(filename, mode);
		//checking mode for writeing
		if (input != null && mode.equals("w"))
			if(!deallocateAllBlocks(input))
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
		 return fileTable.ffree(entry);
	}
	
	int fsize( FileTableEntry ftEnt)
	{
	}
	
	int read (FileTableEntry ftEnt, byte[] buffer )
	{
	}
	int write(FileTableEntry ftEnt, byte[] buffer )
	{
	}
	private boolean deallocAllBlocks(FileTableEntry ftEnt)
	{
	}
	boolean delete ( String filename )
	{
	}
	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	int seek (FileTableEntry ftEnt, int offset, int whence)
	{
	}
}