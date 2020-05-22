package com.wilutions.fx.acpl;

import javafx.scene.control.skin.TextFieldSkin;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

class TextFieldSkinWithImage extends TextFieldSkin {

	StackPane leftPane = new StackPane();

	void setImageView(ImageView imageView) {
		leftPane.getChildren().clear();
		if (imageView != null) {
			imageView.setCursor(Cursor.DEFAULT);
			leftPane.getChildren().add(imageView);
		}
	}

	public TextFieldSkinWithImage(TextField textField) {
		super(textField);
		leftPane.setAlignment(Pos.CENTER_LEFT);
		getChildren().add(leftPane);
		leftPane.setPadding(new Insets(0, 0, 0, 4.0));
	}
	
	private boolean isImageAvailable() {
		return leftPane.getChildren().size() != 0;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		if (isImageAvailable()) {
			final double fullHeight = h + snappedTopInset() + snappedBottomInset();
	
			final double leftWidth = leftPane == null ? 0.0 : snapSize(leftPane.prefWidth(fullHeight));
			final double rightWidth = 0.0;
	
			final double textFieldStartX = snapPosition(x) + snapSize(leftWidth);
			final double textFieldWidth = w - snapSize(leftWidth) - snapSize(rightWidth);
	
			super.layoutChildren(textFieldStartX, 0, textFieldWidth, fullHeight);
	
			final double leftStartX = 0;
			leftPane.resizeRelocate(leftStartX, 0, leftWidth, fullHeight);
		}
		else {
			super.layoutChildren(x, y, w, h);
		}
	}

	// @Override ITJ-87 TODO
	protected int translateCaretPosition(int cp) {
		final double h = getSkinnable().getHeight();
		final double fullHeight = h + snappedTopInset() + snappedBottomInset();
		final double leftWidth = isImageAvailable() ? 0.0 : snapSize(leftPane.prefWidth(fullHeight));
		int offs = (int)leftWidth;
		return Math.max(0, cp - offs);
	}

	// @Override
	protected Point2D translateCaretPosition(Point2D p) {
		int cp = translateCaretPosition((int)p.getX());
		Point2D p1 = new Point2D(cp, p.getY());
		return p1;
	}

}
