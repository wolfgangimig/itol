package com.wilutions.itol;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.swing.filechooser.FileSystemView;

public class FileIconCache {
	static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();

	private static String getFileExt(String fname) {
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}
		else {
			ext = "." + fname;
		}
		return ext.toLowerCase();
	}

	private static javax.swing.Icon getJSwingIconFromFileSystem(File file) {

		// Windows {
		FileSystemView view = FileSystemView.getFileSystemView();
		javax.swing.Icon icon = view.getSystemIcon(file);
		// }

		// OS X {
		// final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		// javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
		// }

		return icon;
	}
	
	public static Image getFileIcon(String ext) {
		ext = getFileExt(ext);
		return getFileIcon(new File("x", ext));
	}

	public static Image getFileIcon(File file) {
		final String ext = getFileExt(file.getName());

		Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
		if (fileIcon == null) {

			javax.swing.Icon jswingIcon = null;

			if (file.exists()) {
				jswingIcon = getJSwingIconFromFileSystem(file);
			} else {
				File tempFile = null;
				try {
					tempFile = File.createTempFile("icon", ext);
					jswingIcon = getJSwingIconFromFileSystem(tempFile);
				} catch (IOException ignored) {
					// Cannot create temporary file.
				} finally {
					if (tempFile != null) {
						tempFile.delete();
					}
				}
			}

			if (jswingIcon != null) {
				fileIcon = jswingIconToImage(jswingIcon);
				mapOfFileExtToSmallIcon.put(ext, fileIcon);
			}
		}

		return fileIcon;
	}

	private static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
		BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		return SwingFXUtils.toFXImage(bufferedImage, null);
	}

}
