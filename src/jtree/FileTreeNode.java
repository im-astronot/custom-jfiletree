package jtree;

import java.io.File;

public class FileTreeNode {
    
    /**
     * Creates a new instance of FileTreeNode
     * @param file The <code>File</code> that will be represented by this class.
     */
    public FileTreeNode(File file) {
        if (file == null)
            throw new IllegalArgumentException("Null file not allowed");

        this.file = file;
    }

    /**
     * returns the representation of this <code>File</code> best suited for use in
     * the <code>FileTree</code>.
     * @return the representation of this <code>File</code> as a <code>String</code>
     */
    public String toString() {
        String name = file.getName();
        if (!Constants.isWindows)
            return name;

        if (name.length() == 0)
            return file.getPath();

        if (Constants.isVista)
        {
            if (name.equals(WINDOWS_MYCOMPUTER)){
                return "Computer";
            }
            
            if ("::{F02C1A0D-BE21-4350-88B0-7367FC96EF3C}".equals(name)){
                return "My Network";
            }
                
            return name;
        }
        
        if (name.equals(WINDOWS_MYCOMPUTER)){
            return "This PC";
        }
        
        if ("::{F02C1A0D-BE21-4350-88B0-7367FC96EF3C}".equals(name)){
            return "Network";
        }
            
        if ("::{031E4825-7B94-4DC3-B131-E946B44C8DD5}".equals(name)){
            return "Library-MS";
        }

        return name;
    }

    /**
     * the object being represented
     */
    public File file;
    /**
     * the hex string that represents 'My Computer ' in Windows
     */
    public static final String WINDOWS_MYCOMPUTER = "::{20D04FE0-3AEA-1069-A2D8-08002B30309D}";
    /**
     * the hex string that represents 'My Network Places ' in Win2k and XP
     */
    public static final String WINDOWS_MYNETWORKPLACES = "::{208D2C60-3AEA-1069-A2D7-08002B30309D}";
    /**
     * the hex string that represents 'Network' in Vista
     */
    public static final String WINDOWSVISTA_NETWORK = "::{F02C1A0D-BE21-4350-88B0-7367FC96EF3C}"; 
}
