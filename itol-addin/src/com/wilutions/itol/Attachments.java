package com.wilutions.itol;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.wilutions.itol.db.Attachment;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Attachments implements Iterable<Attachment> {
	
	private final static Logger log = Logger.getLogger("AttachmentHelper");
	private final static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	ObservableList<Attachment> attachments;

	public Attachments() {
		this.attachments = FXCollections.observableArrayList();
	}
	
	public Attachments(ObservableList<Attachment> attachments) {
		this.attachments = attachments;
	}
	
	public CompletableFuture<List<File>> addAttachmentsFromClipboard() {
		CompletableFuture<List<File>> files = new CompletableFuture<List<File>>();
		try {
			java.awt.Image image = getImageFromClipboard();
			if (image != null) {
				try {
					File file = makeAttachmentFromImage(image);
					files.complete(Arrays.asList(file));
				}
				catch (Exception e) {
					log.log(Level.WARNING, "Failed to paste data from clipboard.", e);
					files.completeExceptionally(e);
				}
			}
			
			List<File> clipFiles = getFilesFromClipboard();
			if (clipFiles != null) {
				files = dropFiles(clipFiles);
			} 
		}
		catch (Exception ex) {
			files.completeExceptionally(ex);
		}
		return files;
	}
	
	private File makeAttachmentFromImage(java.awt.Image image) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("dropImage(" + image);
		// Save clipboard image to temp file
		String iso = DATEFORMAT.format(new Date());
		String fileName = makeUniqueAttachmentFileName(attachments, "image-" + iso + ".png");
		File file = new File(Globals.getTempDir(), fileName);
		writeImageToFile(image, file);
		addFileAsAttachment(file);
		if (log.isLoggable(Level.FINE)) log.fine(")dropImage=" + file);
		return file;
	}

	public CompletableFuture<List<File>> dropFiles(List<File> files) {
		if (log.isLoggable(Level.FINE)) log.fine("dropFiles(" + files);
		
		CompletableFuture<List<File>> fcopies = CompletableFuture.supplyAsync(() -> {
			ArrayList<File> copies = new ArrayList<File>();
			for (File src : files) {
				try {
					String fileName = makeUniqueAttachmentFileName(attachments, src.getName());
					File target = new File(Globals.getTempDir(), fileName);
					if (log.isLoggable(Level.FINE)) log.fine("copy source=" + src + ", target=" + target);
					Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
					addFileAsAttachment(target);
					copies.add(target);
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			return copies;
		});
		
		if (log.isLoggable(Level.FINE)) log.fine(")dropFiles");
		return fcopies;
	}

	private void addFileAsAttachment(File file) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("addFileAsAttachment(" + file);
		Attachment att = MailAttachmentHelper.createFromFile(file);
		attachments.add(att);
		if (log.isLoggable(Level.INFO)) log.info("add attachment=" + att);
		if (log.isLoggable(Level.FINE)) log.fine(")addFileAsAttachment");
	}
	
	public static boolean isImageFile(String fileName) {
		boolean ret = false;
		fileName = fileName.toLowerCase();
		String ext = fileName.substring(fileName.lastIndexOf('.')+1);
		switch (ext) {
		case "png": case "jpg": case "gif":
			ret = true;
			break;
		default:
			ret = false;
		}
		return ret;
	}

	private void writeImageToFile(java.awt.Image image, File file) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("writeImageToFile(image=" + image + ", file=" + file);
		FileOutputStream out = new FileOutputStream(file);
		try {
			ImageIO.write((RenderedImage) image, "png", out);
		}
		finally {
			out.close();
		}
		if (log.isLoggable(Level.FINE)) log.fine(")writeImageToFile");
	}

	/**
	 * Creates a unique file name for an attachment.
	 * @param fileName
	 * @return
	 */
	private String makeUniqueAttachmentFileName(Collection<Attachment> attachments, String fileName) {
		int retry = 0;
		while (findAttachment(attachments, fileName).isPresent()) {
			String name = fileName;
			String ext = "";
			int d = fileName.lastIndexOf('.');
			if (d >= 0) {
				name = fileName.substring(0,  d);
				ext = fileName.substring(d);
			}
			
			// remove already inserted retry counter in file name 
			if (name.endsWith(")")) {
				int p = name.lastIndexOf("(");
				if (p >= 0) {
					int e = name.length()-1;
					String sretry = name.substring(p+1, e);
					try {
						Integer.parseInt(sretry);
						name = name.substring(0, p).trim();
					}
					catch (Exception ignored) {}
				}
			}
			
			fileName = name + " (" + (++retry) + ")" + ext;
		}
		return fileName;
	}

	/**
	 * Find attachment by name.
	 * @param fileName 
	 * @return
	 */
	private Optional<Attachment> findAttachment(Collection<Attachment> attachments, String fileName) {
		String fileNameLC = fileName.toLowerCase();
		Optional<Attachment> ret = attachments.stream().filter(
				(att) -> att.getFileName().toLowerCase().equals(fileNameLC)
				).findFirst();
		return ret;
	}
	
	private java.awt.Image getImageFromClipboard() throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("getImageFromClipboard((");
		java.awt.Image ret = null;
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			ret = (java.awt.Image) transferable.getTransferData(DataFlavor.imageFlavor);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")getImageFromClipboard=" + ret);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private List<File> getFilesFromClipboard() throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("getFilesFromClipboard(");
		List<File> ret = null;
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			ret = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")getFilesFromClipboard=" + ret);
		return ret;
	}

	public ObservableList<Attachment> getObservableList() {
		return attachments;
	}
	
	public void add(Attachment att) {
		getObservableList().add(att);
	}

	public boolean isEmpty() {
		return getObservableList().isEmpty();
	}

	@Override
	public Iterator<Attachment> iterator() {
		return getObservableList().iterator();
	}

	public void addListener(ListChangeListener<Attachment> listChangeListener) {
		getObservableList().addListener(listChangeListener);
	}
}
