package com.wilutions.itol;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class ThumbnailHelper {
	
	private static Logger log = Logger.getLogger("ThumbnailHelper");
	
	public static File makeThumbnail(File file) {
		if (log.isLoggable(Level.FINE)) log.fine("makeThumbnail(" + file);
		File thumbnailFile = null;
		String type = getImageFileType(file);
		if (type.isEmpty()) return thumbnailFile;
		
		final double THUMBNAIL_WIDTH = 200;
		final double THUMBNAIL_HEIGHT = 145;
		
		try {
			Image image = ImageIO.read(file);
			double wd = image.getWidth(null);
			double ht = image.getHeight(null);
			if (log.isLoggable(Level.FINE)) log.fine("image width=" + wd + ", height=" + ht);
			
			// Compute scale width and height
			if (wd <= THUMBNAIL_WIDTH && ht <= THUMBNAIL_HEIGHT) {
				thumbnailFile = file;
			}
			else {
				double ratioX = THUMBNAIL_WIDTH/wd;
				double ratioY = THUMBNAIL_HEIGHT/ht;
				double ratio  = ratioX;
				if (ratioY < ratio) {
					ratio = ratioY;
				}
				
				if (log.isLoggable(Level.FINE)) log.fine("scale image ratio=" + ratio);
				BufferedImage thumbnail = scale((BufferedImage)image, ratio);
				
				thumbnailFile = makeThumbnailFileName(file);
				ImageIO.write(thumbnail, "png", thumbnailFile);
			}
			
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to create thumbnail of " + file, e);
			thumbnailFile = null;
		}
		
		if (log.isLoggable(Level.FINE)) log.fine(")makeThumbnail=" + thumbnailFile);
		return thumbnailFile;
	}
	
	/**
	 * Make file name as imageFile-thumbnail.png
	 * @param imageFile
	 * @return
	 */
	private static File makeThumbnailFileName(File imageFile) {
		String fileName = imageFile.getName();
		String fname = fileName;
		String ext = "";
		int p = fileName.lastIndexOf('.');
		if (p >= 0) {
			fname = fileName.substring(0, p);
			ext = fileName.substring(p);
		}
		fname += "-thumbnail" + ext;
		return new File(imageFile.getParentFile(), fname);
	}
	
	/**
	 * Scale image by given ratio.
	 * Algorithm found at
	 * http://stackoverflow.com/questions/1069095/how-do-you-create-a-thumbnail-image-out-of-a-jpeg-in-java
	 * @param source
	 * @param ratio
	 * @return Scaled image
	 */
	private static BufferedImage scale(BufferedImage source, double ratio) {
	  int w = (int) (source.getWidth() * ratio);
	  int h = (int) (source.getHeight() * ratio);
	  BufferedImage bi = getCompatibleImage(w, h);
	  Graphics2D g2d = bi.createGraphics();
	  double xScale = (double) w / source.getWidth();
	  double yScale = (double) h / source.getHeight();
	  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
	  g2d.drawRenderedImage(source, at);
	  g2d.dispose();
	  return bi;
	}

	private static BufferedImage getCompatibleImage(int w, int h) {
	  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	  GraphicsDevice gd = ge.getDefaultScreenDevice();
	  GraphicsConfiguration gc = gd.getDefaultConfiguration();
	  BufferedImage image = gc.createCompatibleImage(w, h);
	  return image;
	}
		
	/**
	 * Return image file type.
	 * @param file
	 * @return png, jpg or ""
	 */
	private static String getImageFileType(File file) {
		String fname = file.getName();
		String type = "";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			type = fname.substring(p+1).toLowerCase();
		}
		switch (type) {
		case "png": case "jpg": break;
		default: type = "";
		}
		return type;
	}
	
	@SuppressWarnings("unused")
	private static javafx.scene.image.Image awtImageToFX(java.awt.Image image) throws Exception {
		if (!(image instanceof RenderedImage)) {
			BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_INT_ARGB);
			Graphics g = bufferedImage.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();

			image = bufferedImage;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write((RenderedImage) image, "png", out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		return new javafx.scene.image.Image(in);
	}

}
