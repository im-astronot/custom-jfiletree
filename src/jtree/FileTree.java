package jtree;

import java.awt.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.JFileChooser;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class FileTree extends JTree {
    
    /** Creates a new instance of FileTree */
    public FileTree() {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("root")));
        fileTreeModel = (DefaultTreeModel)treeModel;
        showHiddenFiles = false;
        showFiles = true;
        navigateOSXApps = false;

        initComponents();
        initListeners();
    }
    
    /**
     * returns the data model used by the FileTree. This method returns the same value
     * as <code>getModel()</code>, with the only exception being that this method
     * returns a <code>DefaultTreeModel</code>
     * @return the data model used by the <code>FileTree</code>
     */
    public DefaultTreeModel getFileTreeModel() {
        return fileTreeModel;
    }
    
    /**
     * returns the selected file in the tree. If there are multiple selections in the
     * tree, then it will return the <code>File</code> associated with the value
     * returned from <code>getSelectionPath</code>. You can enable/disable mutliple
     * selections by changing the mode of the <code>TreeSelectionModel</code>.
     * @return the selected file in the tree
     */
    public File getSelectedFile() {
        TreePath treePath = getSelectionPath();
        if (treePath == null)
            return null;
        
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        FileTreeNode fileTreeNode = (FileTreeNode)treeNode.getUserObject();
        return fileTreeNode.file;
    }
    
    /**
     * returns an array of the files selected in the tree. To enable/disable multiple
     * selections, you can change the selection mode in the
     * <code>TreeSelectionModel</code>.
     * @return an array of the files selected in the tree
     */
    public File[] getSelectedFiles() {
        TreePath[] treePaths = getSelectionPaths();
        if (treePaths == null)
            return null;
        
        File [] files = new File[treePaths.length];
        for (int i=0; i<treePaths.length; i++)
        {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)treePaths[i].getLastPathComponent();
            FileTreeNode fileTreeNode = (FileTreeNode)treeNode.getUserObject();
            files[i] = fileTreeNode.file;
        }
        
        return files;
    }
    
    /**
     * initializes class members
     */
    private void initComponents() {
        if (Constants.isWindows)
            fsv = FileSystemView.getFileSystemView();
        
        initRoot();
        setCellRenderer(new FileTreeCellRenderer());
        setEditable(false);
        
        
    }
    
    /**
     * sets up the listeners for the tree
     */
    private void initListeners() {
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
            }
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                treeNode.removeAllChildren();
                populateSubTree(treeNode);
                fileTreeModel.nodeStructureChanged(treeNode);
            }
        });
        
        FileTreeListener ftl = new FileTreeListener(this);
        addMouseListener(ftl);
    }

    
    private void initRoot() {
    File[] roots = null;
    if (Constants.isWindows) {
        roots = fsv.getRoots();
    } else if (Constants.isLinux || Constants.isOSX) {
        roots = new File[]{new File(System.getProperty("user.home"))};
    } else {
        roots = File.listRoots();
    }
        
    if (roots.length == 1) {
        rootNode = new DefaultMutableTreeNode(new FileTreeNode(roots[0]));
        populateSubTree(rootNode);
    } else if (roots.length > 1) {
        rootNode = new DefaultMutableTreeNode("Computer");
        for (File root : roots) {
            rootNode.add(new DefaultMutableTreeNode(root));
        }
    } else {
        rootNode = new DefaultMutableTreeNode("Error");
    }
        
    fileTreeModel.setRoot(rootNode);
}

    /**
     * returns true if deleting is allowed in the tree, false otherwise. The default
     * value is false.
     * @return true if deleting is allowed in the tree, false otherwise
     */
    public boolean isDeleteEnabled() {
        return allowDelete;
    }
    
    /**
     * returns true if the user can navigate into OS X application bundles. false
     * otherwise
     * @return true if the user can navigate into OS X application bundles
     */
    public boolean isNavigateOSXApps() {
        return navigateOSXApps;
    }
    
    /**
     * returns true if files will be shown in the tree, false otherwise. Default value
     * is true.
     * @return true if files will be shown in the tree, false otherwise
     */
    public boolean isShowFiles() {
        return showFiles;
    }
    
    /**
     * returns true if the tree will show hidden files, false otherwise. Default value
     * is false.
     * @return true if the tree will show hidden files, false otherwise
     */
    public boolean isShowHiddenFiles() {
        return showHiddenFiles;
    }
    
    /**
     * called whenever a node is expanded
     * @param node the node to expand
     */
    private void populateSubTree(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof FileTreeNode)
        {
            FileTreeNode fileTreeNode = (FileTreeNode)userObject;
            File []files = fileTreeNode.file.listFiles();
            
            // Windows displays directories before regular files, so we're going
            // to sort the list of files such that directories appear first
            if (Constants.isWindows)
            {
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        boolean f1IsDir = f1.isDirectory();
                        boolean f2IsDir = f2.isDirectory();
                        
                        if (f1IsDir == f2IsDir)
                            return f1.compareTo(f2);
                        if (f1IsDir && !f2IsDir)
                            return -1;
                        
                        // here we assume that f1 is a file, and f2 is a directory
                        return 1;
                    }
                });
            }
            else
                Arrays.sort(files);
            
            for (File file:files)
            {
                if (file.isFile() && !showFiles)
                    continue;
                
                if (!showHiddenFiles && file.isHidden())
                    continue;
                
                FileTreeNode subFile = new FileTreeNode(file);
                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subFile);
                if (file.isDirectory())
                {
                    if (!Constants.isOSX || navigateOSXApps || !file.getName().endsWith(".app"))
                        subNode.add(new DefaultMutableTreeNode("Fake"));
                }
                node.add(subNode);
            }
        }
    }
    
    /**
     * Expands the tree to the <code>File</code> specified by the argument, and selects
     * it as well. If the <code>currFile</code> does not exist or is null, calling this
     * method will have no effect.
     * @param currFile The file or directory to expand the tree to and select.
     */
    public void setCurrentFile(File currFile) {
        if (currFile == null || !currFile.exists())
            return;
        
        String path = currFile.getPath();
        String [] pathParts = null;
        if (Constants.isWindows)
            pathParts = path.split("\\\\");
        else
            pathParts = path.split(File.separator);
        
        if (Constants.isWindows)
        {
            int childCount = rootNode.getChildCount();
            DefaultMutableTreeNode myComputer = null;
            for (int i=0; i<childCount; i++)
            {
                FileTreeNode fileTreeNode = 
                        (FileTreeNode)((DefaultMutableTreeNode)rootNode.getChildAt(i)).getUserObject();
                if (fileTreeNode.file.getPath().equals(FileTreeNode.WINDOWS_MYCOMPUTER))
                {
                    myComputer = (DefaultMutableTreeNode)rootNode.getChildAt(i);
                    TreePath treePath = new TreePath(myComputer.getPath());
                    expandPath(treePath);
                    break;
                }
            }
            
            DefaultMutableTreeNode currNode = myComputer;
            for (String part:pathParts)
            {
                childCount = currNode.getChildCount();
                for (int i=0; i<childCount; i++)
                {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)currNode.getChildAt(i);
                    FileTreeNode fileTreeNode = (FileTreeNode)childNode.getUserObject();
                    String pathName = fileTreeNode.file.getName();
                    if (pathName.length() == 0)
                        pathName = fileTreeNode.file.getPath().substring(0, 2);
                    if (pathName.equals(part))
                    {
                        TreePath treePath = new TreePath(childNode.getPath());
                        expandPath(treePath);
                        selectionModel.setSelectionPath(treePath);
                        currNode = childNode;
                        break;
                    }
                }
            }
            
        }
        else
        {
            DefaultMutableTreeNode currNode = rootNode;
            for (String part:pathParts)
            {
                int childCount = currNode.getChildCount();
                for (int i=0; i<childCount; i++)
                {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)currNode.getChildAt(i);
                    FileTreeNode fileTreeNode = (FileTreeNode)childNode.getUserObject();
                    if (fileTreeNode.file.getName().equals(part))
                    {
                        TreePath treePath = new TreePath(childNode.getPath());
                        expandPath(treePath);
                        selectionModel.setSelectionPath(treePath);
                        currNode = childNode;
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Allow or disallow the user to delete files from the tree view.
     * @param allowDelete <code>true</code> allows deleting of files/directories. <code>false</code> does
     * not.
     */
    public void setDeleteEnabled(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }
    
    /**
     * Toggle the showing of files in the tree (as opposed to just directories)
     * @param showFiles <code>true</code> shows files in the tree. <code>false</code> does not.
     */
    public void setShowFiles(boolean showFiles) {
        if (this.showFiles != showFiles)
        {
            this.showFiles = showFiles;
            initRoot();
        }
    }
    
    /**
     * Allows or disallows the showing of hidden files and directories in the tree.
     * @param showHiddenFiles <code>true</code> shows hidden files. <code>false</code> does not.
     */
    public void setShowHiddenFiles(boolean showHiddenFiles) {
        if (showHiddenFiles != this.showHiddenFiles)
        {
            this.showHiddenFiles = showHiddenFiles;
            initRoot();
        }
    }
    
    /**
     * sets whether the user can navigate into OS X application bundles (.app). The
     * default value is <code>false</code>
     * @param navigateOSXApps if true, users will be able to navigate into OS X application bundles. set it
     * to false to disallow navigating bundles.
     */
    public void setNavigateOSXApps(boolean navigateOSXApps) {
        this.navigateOSXApps = navigateOSXApps;
    }
    
    /**
     * the root node of the <code>FileTree</code>
     */
    protected DefaultMutableTreeNode rootNode;
    /**
     * the <code>TreeModel</code> for this object. The same value as the <code>JTree</code>
     * treeModel member.
     */
    protected DefaultTreeModel fileTreeModel;
    /**
     * just a filesystemview used to get icons for nodes in Windows
     */
    protected FileSystemView fsv;
    /**
     * whether or not to show hidden files
     */
    protected boolean showHiddenFiles;
    /**
     * whether or not to show files
     */
    protected boolean showFiles;
    /**
     * whether to allow deleting of files
     */
    protected boolean allowDelete;
    /**
     * allows/disallows navigating into OS X application bundles
     */
    protected boolean navigateOSXApps;
    
    /**
     * A subclass of DefaultTreeCellRenderer that is responsible for rendering the
     * nodes and their icons
     */
    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        /**
         * just a simple constructor
         */
        public FileTreeCellRenderer() {
            fileChooser = new JFileChooser();
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
            if (userObject instanceof FileTreeNode)
            {
                FileTreeNode fileTreeNode = (FileTreeNode)userObject;
                
                if (!Constants.isWindows)
                {
                    try { setIcon(fileChooser.getIcon(fileTreeNode.file)); }
                    catch (Exception e) { e.printStackTrace(); }
                }
                else
                {
                    try { setIcon(fsv.getSystemIcon(fileTreeNode.file)); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            }

            return this;
        }
        
        /**
         * used to obtain icons for non-Windows OSes
         */
        private JFileChooser fileChooser;
    }
    
    public void startFileWatcher() {
    try {
        File file= null;
        // Get the file system
        FileSystem fs = file.toPath().getFileSystem();
        
        // Create a new watch service
        WatchService service = fs.newWatchService();
        
        // Register the file to be watched for changes
        file.toPath().register(service, StandardWatchEventKinds.ENTRY_CREATE, 
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        
        // Start the infinite loop that will check for file system changes
        
            WatchKey key = service.take();
            
            // Process each event in the watch key
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                // Handle file create event
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    // Update the file tree
                    updateFileTree(file);
                }
                
                // Handle file delete event
                else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    // Update the file tree
                    updateFileTree(file);
                }
                
                // Handle file modify event
                else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    // Update the file tree
                    updateFileTree(file);
                }
            }
            
        
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
}

public void updateFileTree(File file) {
    // Get the current root node
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();
    
    // Clear the children of the root node
    rootNode.removeAllChildren();
    
    // Add the children of the file to the root node
    addChildren(file, rootNode);
    
    // Update the tree
    ((DefaultTreeModel) getModel()).reload();
    
    // Expand the root node to show any newly added child nodes
    expandRow(0);
}

public void addChildren(File file, DefaultMutableTreeNode parent) {
    // Get the files and directories in the current directory
    File[] files = file.listFiles();
    
    // Sort the files and directories alphabetically
    Arrays.sort(files);
    
    // Add each file and directory to the parent node
    for (File child : files) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileTreeNode(child));
        parent.add(node);
        if (child.isDirectory()) {
            addChildren(child, node);
        }
    }
}
  
}
