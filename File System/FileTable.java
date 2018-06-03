import java.util.Vector;

public class FileTable {

   private Vector<FileTableEntry> table;         // the actual entity of this file table
   private Directory dir;        // the root directory 

   public FileTable( Directory directory ) { // constructor
      table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
      dir = directory;           // receive a reference to the Director
   }                             // from the file system

   // major public methods
   public synchronized FileTableEntry falloc( String filename, String mode ) {
      // allocate a new file (structure) table entry for this file name
      // allocate/retrieve and register the corresponding inode using dir
      // increment this inode's count
      // immediately write back this inode to the disk
      // return a reference to this file (structure) table entry
      
      short inumber = dir.namei(filename);
      Inode inode = new Inode(inumber);
      inode.count++;
      inode.toDisk(inumber);
      FileTableEntry entry = new FileTableEntry(inode, inumber, filename);
      table.add(entry);
      return entry;
   }

   public synchronized boolean ffree( FileTableEntry e ) {
      // receive a file table entry reference
      // save the corresponding inode to the disk
      // free this file table entry.
      // return true if this file table entry found in my table
      
      short inumber = e.iNumber;
      Inode inode = e.inode;
      inode.toDisk(inumber);
      int index = table.indexOf(e);
      if (index == -1) {
      	return false;
      }
      else {
      	table.remove(index);
      	return true;
      }
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  // return if table is empty 
   }                            // should be called before starting a format

	public synchronized Inode getInode(int inumber)
	{
		for(int i =0; i< table.length; i++)
		{
			FileTableEntry temp = table.elementAt(i);
			if(inumber == temp.iNumber)
			{
				return temp.getInode();
			}
		}
		return null;
	}
}
