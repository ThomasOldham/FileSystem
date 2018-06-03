import java.util.Stack;

public class Directory {
   private final static int MAX_CHARS = 30; // max characters of each file name
   private final static int SHORT_SIZE = 2;

   // Directory entries
   private short fsize[];        // each element stores a different file size.
   private char fnames[][];    // each element stores a different file name.
   private Stack<Short> availableFids;

   public Directory( int maxInumber ) { // directory constructor
      fsize = new short[maxInumber];     // maxInumber = max files
      for ( short i = 0; i < maxInumber; i++ ) 
         fsize[i] = 0;                 // all file size initialized to 0
      fnames = new char[maxInumber][MAX_CHARS];
      String root = "/";                // entry(inode) 0 is "/"
      fsize[0] = 1;        // fsize[0] is the size of "/".
      root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
      availableFids = new Stack<Short>();
      for (short i = 1; i < maxInumber; i++) {
		availableFids.push(i);
	  }
   }

   public void bytes2directory( byte data[] ) {
      // assumes data[] received directory information from disk
      // initializes the Directory instance with this data[]
      for (short fileNum = 0; fileNum < fsize.length; fileNum++) {
      	int sizeOffset = fileNum * SHORT_SIZE;
      	int nameOffset = fsize.length * SHORT_SIZE + fileNum;
      	fsize[fileNum] = SysLib.bytes2short(data, sizeOffset);
      	if (fsize[fileNum] == 0) {
      		availableFids.push(fileNum);
      	}
      	else for (int charNum = 0; charNum < fsize.length; charNum++) {
      		fnames[fileNum][charNum] = (char)(data[nameOffset + charNum]);
      	}
      }
   }

   public byte[] directory2bytes( ) {
      // converts and return Directory information into a plain byte array
      // this byte array will be written back to disk
      // note: only meaningfull directory information should be converted
      // into bytes.
      byte[] data = new byte[fsize.length * (SHORT_SIZE + MAX_CHARS)];
      for (int fileNum = 0; fileNum < fsize.length; fileNum++) {
      	int sizeOffset = fileNum * SHORT_SIZE;
      	int nameOffset = fsize.length * SHORT_SIZE + fileNum;
      	SysLib.short2bytes(fsize[fileNum], data, sizeOffset);
      	for (int charNum = 0; charNum < fsize.length; charNum++) {
      		data[nameOffset + charNum] = (byte)(fnames[fileNum][charNum]);
      	}
      }
      return data;
   }

   public short ialloc( String filename ) {
      // filename is the one of a file to be created.
      // allocates a new inode number for this filename
      if (availableFids.isEmpty()) {
      	return -1;
      }
      else {
      	short fid = availableFids.pop();
      	fsize[fid] = 1;
      	fnames[fid] = filename.toCharArray();
      	return fid;
      }
   }

   // Returns true if it deleted the file, false if it didn't exist.
   public boolean ifree( short iNumber ) {
      // deallocates this inumber (inode number)
      // the corresponding file will be deleted.
      boolean deleted = (fsize[iNumber] == 0);
      fsize[iNumber] = 0;
      if (deleted) {
      	availableFids.push(iNumber);
      }
      return deleted;
   }

   public short namei( String filename ) {
      // returns the inumber corresponding to this filename
      for (short i = 0; i < fnames.length; i++) {
      	if (fnames[i] == filename.toCharArray()) {
      		return i;
      	}
      }
      return -1;
   }
}
