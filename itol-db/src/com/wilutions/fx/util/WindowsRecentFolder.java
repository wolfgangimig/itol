package com.wilutions.fx.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.itol.db.Default;

// https://msdn.microsoft.com/de-de/library/windows/desktop/dd378457.aspx
// http://www.codejava.net/java-se/file-io/file-change-notification-example-with-watch-service-api
public class WindowsRecentFolder implements Closeable {
	
	private final File recentFolder;
	private final int watchMaxFiles;
	private final TreeSet<File> recentFiles = new TreeSet<File>(CompareFileByLastModifiedDecending.instance);
	private WatchThread watchThread;
	private static Logger log = Logger.getLogger("WindowsRecentFolder");
	
	public final static int FOLDERS = 1;
	public final static int FILES = 2;
	public final static int FILES_AND_FOLDERS = FOLDERS | FILES;
	
	public WindowsRecentFolder() {
		this(Integer.MAX_VALUE);
	}
	
	public WindowsRecentFolder(int watchMaxFiles) {
		this.watchMaxFiles = watchMaxFiles;
		recentFolder = getRecentFolderPath();
		watchThread = new WatchThread();
		watchThread.start();
	}
	
	public List<File> getFiles(int maxFiles, int filesOrFolders) {
		List<File> links = null;
		synchronized(recentFiles) {
			links = new ArrayList<File>(recentFiles);
		}
		
		List<File> ret = new ArrayList<File>(links.size());
		for (File link : links) {
			try {
				if (WindowsShortcut.isPotentialValidLink(link)) {
					WindowsShortcut wlink = new WindowsShortcut(link);
					String fname = wlink.getRealFilename();
					File file = new File(fname);
					boolean add = (filesOrFolders == FILES_AND_FOLDERS) ||
						((filesOrFolders & FILES) != 0 && file.isFile()) ||
						((filesOrFolders & FOLDERS) != 0 && file.isDirectory());
					if (add) {
						ret.add(file);
						if (log.isLoggable(Level.INFO)) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
							String lastModifiedIso = sdf.format(new Date(link.lastModified()));
							System.out.println("Recent file=" + link + ", lastModified=" + lastModifiedIso);
						}
						if (ret.size() >= maxFiles) break;
					}
				}					
			} catch (Exception e) {
			}
		}
		
		
		return ret;
	}

	@Override
	public void close() throws IOException {
		watchThread.done();
	}
	
	private static class CompareFileByLastModifiedDecending implements Comparator<File> {
		public final static CompareFileByLastModifiedDecending instance = new CompareFileByLastModifiedDecending();
		public int compare(File lhs, File rhs) {
			long cmp = rhs.lastModified() - lhs.lastModified();
			if (cmp < 0) return -1;
			if (cmp > 0) return 1;
			return 0;
		}
	}

	private void unsync_shrinkFileListToMax(TreeSet<File> files) {
		int index = 0;
		Iterator<File> it = files.iterator();
		while(index++ < watchMaxFiles && it.hasNext()) {
			it.next();
		}
		
		while(it.hasNext()) {
			it.next();
			it.remove();
		}
	}
	
	private class WatchThread extends Thread {
		
		private WatchService watcher;
		
		public WatchThread() {
			setName("watch-recent-files");
			setDaemon(true);
			init();
		}
		
		public synchronized void done() {
			try {
				watcher.close();
			} catch (IOException e) {
			}
		}
		
		private synchronized void init() {
			try {
				watcher = FileSystems.getDefault().newWatchService();
			} catch (Exception e) {
				log.log(Level.WARNING, "Watching directory=" + recentFolder + " failed.", e);
			}
		}
		
		public void run() {
			
			try {
				
				// Initial file list
				synchronized(recentFiles) {
					recentFiles.addAll(Arrays.asList(recentFolder.listFiles()));
					unsync_shrinkFileListToMax(recentFiles);
				}
				
			    recentFolder.toPath().register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			    
			    while (!Thread.currentThread().isInterrupted()) {
			    	
			    	WatchKey key = watcher.take();
			    	
			    	ArrayList<File> modifiedFiles = new ArrayList<>();
			    	ArrayList<File> deletedFiles = new ArrayList<>();
			    	
			    	for (WatchEvent<?> event : key.pollEvents()) {

			    		WatchEvent.Kind<?> kind = event.kind();
			    		if (kind != OVERFLOW) {
			     
				            @SuppressWarnings("unchecked")
				            WatchEvent<Path> ev = (WatchEvent<Path>) event;
				            File file = new File(recentFolder, ev.context().toFile().getName());

				            if (kind == ENTRY_DELETE) {
				            	deletedFiles.add(file);
				            }
				            else {
				            	modifiedFiles.add(file);
				            }
			            }
			    		
			        }
			    	
			    	
			    	synchronized(recentFiles) {
			    		recentFiles.removeAll(deletedFiles);
			    		recentFiles.removeAll(modifiedFiles);
			    		recentFiles.addAll(modifiedFiles);
			    		unsync_shrinkFileListToMax(recentFiles);
			    	}

			    	if (!key.reset()) break;
			    }
			}
			catch (InterruptedException e) {
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Watching directory=" + recentFolder + " failed.", e);
			}
			finally {
				done();
			}
		}
	}
	
	private static File getRecentFolderPath() {
		File ret = null;
		
		// %APPDATA%\Microsoft\Windows\Recent
		String appData = Default.value(System.getenv("APPDATA"));
		if (appData.isEmpty()) {
			// legacy location: %USERPROFILE%\Recent
			String userProfile = Default.value(System.getenv("USERPROFILE"));
			if (userProfile.isEmpty()) {
				File dir = new File(userProfile, "Recent");
				if (dir.exists()) {
					ret = dir;
				}
			}
		}
		else {
			File dir = new File(new File(new File(appData, "Microsoft"), "Windows"), "Recent");
			if (dir.exists()) {
				ret = dir;
			}
		}
		
		return ret;
	}

}
