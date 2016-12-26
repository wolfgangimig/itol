package com.wilutions.fx.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

// http://stackoverflow.com/questions/28493097/is-there-any-date-and-time-picker-available-for-javafx
public class DateTimePicker extends DatePicker {
	public static final String DefaultFormat = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
			  FormatStyle.MEDIUM, FormatStyle.SHORT, IsoChronology.INSTANCE, 
			  Locale.GERMANY); // or whatever Locale

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DefaultFormat);
	
	private ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>(LocalDateTime.now());
	
	private ObjectProperty<String> format = new SimpleObjectProperty<String>() {
		public void set(String newValue) {
			super.set(newValue);
			formatter = DateTimeFormatter.ofPattern(newValue);
		}
	};

	public DateTimePicker() {
		getStyleClass().add("datetime-picker");
		setConverter(new InternalConverter());

		// Syncronize changes to the underlying date value back to the
		// dateTimeValue
		valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				dateTimeValue.set(null);
			} else {
				if (dateTimeValue.get() == null) {
					dateTimeValue.set(LocalDateTime.of(newValue, LocalTime.now()));
				} else {
					LocalTime time = dateTimeValue.get().toLocalTime();
					dateTimeValue.set(LocalDateTime.of(newValue, time));
				}
			}
		});

		// Syncronize changes to dateTimeValue back to the underlying date value
		dateTimeValue.addListener((observable, oldValue, newValue) -> {
			setValue(newValue == null ? null : newValue.toLocalDate());
		});

		// Persist changes onblur
		getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue)
				simulateEnterPressed();
		});

	}

	private void simulateEnterPressed() {
		getEditor().fireEvent(new KeyEvent(getEditor(), getEditor(), KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER,
				false, false, false, false));
	}

	public LocalDateTime getDateTimeValue() {
		return dateTimeValue.get();
	}

	public void setDateTimeValue(LocalDateTime dateTimeValue) {
		if (dateTimeValue.isAfter(LocalDateTime.of(1971, 6, 30, 12, 00)))
			this.dateTimeValue.set(dateTimeValue);
		else
			this.dateTimeValue.set(null);
	}

	public ObjectProperty<LocalDateTime> dateTimeValueProperty() {
		return dateTimeValue;
	}

	public String getFormat() {
		return format.get();
	}

	public ObjectProperty<String> formatProperty() {
		return format;
	}

	public void setFormat(String format) {
		this.format.set(format);
	}

	class InternalConverter extends StringConverter<LocalDate> {
		public String toString(LocalDate object) {

			LocalDateTime value = getDateTimeValue();
			return (value != null) ? value.format(formatter) : "";
		}

		public LocalDate fromString(String value) {
			if (value == null) {
				dateTimeValue.set(null);
				return null;
			}

			dateTimeValue.set(LocalDateTime.parse(value, formatter));
			return dateTimeValue.get().toLocalDate();
		}
	}
}
