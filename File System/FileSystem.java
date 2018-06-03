

public class FileSystem {

	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	private final int MAXFILESIZE = (267 * 512) //11 direct, 1 indirect that stores 256 pointers, each pointing to a block of 512 bytes

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
	
	//sync the file systems metadata into the disk
	void sync() 
	{
		FileTableEntry ftEntry = open("/", "w");		//read directory from disk
		byte[] buffer = directory.directory2bytes();
		write(ftEntry, buffer);
		close(ftEntry);
		superblock.sync();					//write superblock back to disk
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
	
	//return the size of the file
	int fsize( FileTableEntry ftEnt)
	{
		synchronized (ftEnt)
		{
			return ftEnt.inode.lenght;
		}		
	}
	//???????????????????????????????????????????????
	int read (FileTableEntry ftEnt, byte[] buffer )
	{
		return 1;
	}

	int write(FileTableEntry ftEnt, byte[] buffer )
	{
		if(freelist == -1 || (buffer.length > MAXFILESIZE)	//no blocks free or file is too large
			return -1;
		
		synchronized(ftEnt)
		{
			int filesize = fsize(ftEnt);
			int remainingWrite = buffer.length;
			int bytesWritten;
			while(remainingWrite > 0)
			{
				int blockID = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
			}
		}

	}

	
	private boolean deallocAllBlocks(FileTableEntry ftEnt)
	{
		if(ftEnt != null && entry.inode.count == 1)
		{
			if(ftEnt.inode.getIndexBlockNumber() != -1) //Take care of blocks in indirect index
			{
				byte[] tempBuffer = new byte[Disk.blockSize]; //create temp buffer to store indirect index
				SysLib.rawread(indirect, tempBuffer);
				int offset = 0;
				short tempBlock = SysLib.bytes2Short(tempBuffer, 0);	//Get first block in index
				while(tempBlock != 1 && offset < Disk.blockSize)
				{
					superblock.returnBlock(tempBlock);	//return block to freelist
					offset += 2;				//move to next short
					tempBlock = SysLib.bytes2Short(tempBuffer, offset);
				} 
				superblock.returnBlock(ftEnt.inode.indirect);	//return the block that stores indirect index
			}
			
			//release the direct blocks
			for(short i = 0; i < ftEnt.inode.direct.length; i++)
			{
				if(ftEnt.inode.direct[i] != -1)	//there is a block in this entry
				{
					superblock.returnBlock(ftEnt.inode.direct[i]);
					ftEnt.inode.direct[i] = -1;
				}
			}
			return true;
		}
		return false;
	}

	//delete the file 
	boolean delete ( String filename )
	{
		short index = directory.namei(filename);		//search the directory and find the inumber associated with this file
		if(index != -1)						//if the file have been found
		{
			Inode tempNode = FileTableEntry.getInode(index);	//retrieving index inode from disk
			inode.flag = 4;					//flag 4 will be assign for delete
			if(inode.count ==0)				//if there is no file-table entries
			{
				directory.ifree(index);			//deallocates this inumber
			}
			return true;
		}

		return false;
	}

	//update the seek pointer	
	int seek (FileTableEntry ftEnt, int offset, int whence)
	{
		if(ftEnt = null)
		{
			SysLib.cerr("Invalid File Location");
			return -1;
		}
		
		synchronized(ftEnt)
		{
			//checking the whence situation
			switch (whence)
			{
				case SEEK_END:				//set offset from end of the file
					if(offset > 0)
						return -1;
					ftEnt.seekPtr = fsize(ftEnt) + offset;
					break;
				case SEEK_CUR:				//set offset from current position of the file
					if(ftEnt.seekPtr + offset > fsize(ftEnt))
						return -1;
					ftEnt.seekPtr += offset;
					break;
				case SEEK_SET:				//set offset from begining of the file
					if(offset < 0)
						return -1;				
					ftEnt.seekPtr = offset;
					break;
			}
			return ftEnd.seekPtr;
		}
	}






}
