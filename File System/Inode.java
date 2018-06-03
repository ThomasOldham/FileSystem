public class Inode 
{
   private final static int iNodeSize = 32;       // fix to 32 bytes
   private final static int directSize = 11;      // # direct pointers
   
   private final static int INT_SIZE = 4;
   private final static int SHORT_SIZE = 2;
   private final static int FLAG_START = 0;
   private final static int LENGTH_START = FLAG_START + SHORT_SIZE;
   private final static int COUNT_START = LENGTH_START + INT_SIZE;
   private final static int DIRECT_START = COUNT_START + SHORT_SIZE;
   private final static int INDIRECT_START = DIRECT_START + SHORT_SIZE * directSize;
   private static final int DISK_OFFSET = 1;
   private static final int BLOCK_SIZE = 512;
   
   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                         // a indirect pointer

	Inode( ) 
	{                                     // a default constructor
		length = 0;
		count = 0;
		flag = 1;
		for ( int i = 0; i < directSize; i++ )
		direct[i] = -1;
		indirect = -1;
	}

	Inode( short iNumber ) 
	{                       // retrieving inode from disk
		byte[] buffer = new byte[BLOCK_SIZE];
		int blockNumber = DISK_OFFSET + iNumber / (BLOCK_SIZE / iNodeSize);
		int blockOffset = iNumber % (BLOCK_SIZE / iNodeSize);
		SysLib.cread(blockNumber, buffer);
		length = SysLib.bytes2int(buffer, blockOffset + LENGTH_START);
		count = SysLib.bytes2short(buffer, blockOffset + COUNT_START);
		flag = SysLib.bytes2short(buffer, blockOffset + FLAG_START);
		for (int i = 0; i < directSize; i++) 
		{
			direct[i] = SysLib.bytes2short(buffer, blockOffset + DIRECT_START + i * SHORT_SIZE);
		}
		indirect = SysLib.bytes2short(buffer, blockOffset + INDIRECT_START);
	}

	int toDisk( short iNumber ) 
	{       // save to disk as the i-th inode
		byte[] buffer = new byte[BLOCK_SIZE];
		int blockNumber = DISK_OFFSET + iNumber / (BLOCK_SIZE / iNodeSize);
		int blockOffset = iNumber % (BLOCK_SIZE / iNodeSize);
		SysLib.cread(blockNumber, buffer);
		SysLib.int2bytes(length, buffer, blockOffset + LENGTH_START);
		SysLib.short2bytes(count, buffer, blockOffset + COUNT_START);
		SysLib.short2bytes(flag, buffer, blockOffset + FLAG_START);

		for (int i = 0; i < directSize; i++) 
		{
			SysLib.short2bytes(direct[i], buffer, blockOffset + DIRECT_START + i * SHORT_SIZE);
		}

		SysLib.short2bytes(indirect, buffer, blockOffset + INDIRECT_START);
		SysLib.cwrite(blockNumber, buffer);

		//delete
		return 0;
	}

	public short getIndexBlockNumber()
	{
		return indirect;
	}

	public Boolean setIndexBlock(short indexBlockNumber)
	{
		if(indexBlockNumber != -1)
		{
			indirect = indexBlockNumber;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public Boolean addBlock(short freeBlock)
	{
		//check if any direct blocks are open
		short index = 0;
		while(direct[index] != -1 && index <= directSize)
			index++;
		if(index < directSize)		//add block to first free direct index
			direct[index] = freeBlock;
		else
		{
			if(indirect == -1) //indirect has not yet been allocated
			{
				setIndexBlock(SuperBlock.getBlock());
			}
			byte[] tempBuffer = new byte[Disk.blockSize]; //create temp buffer to store indirect index
			SysLib.rawread(indirect, tempBuffer);		
			int counter = 0;				//keeps track of where you are in indirect index
			short tempID = SysLib.bytes2short(tempBuffer, 0);
			while(tempID != -1)				//traverse until you find a free spot
			{
				if(counter > Disk.blockSize)	//out of bounds
					return false;
				else
				{
					counter += 2;
					tempID = SysLib.bytes2short(tempBuffer, counter);
				}
			} //found index to store free block in indirect
			SysLib.short2bytes(freeBlock, tempBuffer, counter);
			SysLib.rawwrite(indirect, tempBuffer);		//write temporary buffer to disk
		}				
		return true;
	}
	
	short findTargetBlock( int offset )
	{
		short blk = offset/Disk.blockSize;
		if(blk >= 11)	//we are in the indirect index
		{
			byte[] indexBlock = new byte[Disk.blockSize];	//buffer to read indirect index
			SysLib.rawread(indirect, indirectData);		//read index into buffer
			int indexOffset = (blk - directSize) * 2;	//Multipled by 2 because pointers are 2 bytes
			blk = SysLib.bytes2short(indirectData, offset); // 
			return blk;
		}
		else
		{
			return direct[blk];
		}
		
	}
}
