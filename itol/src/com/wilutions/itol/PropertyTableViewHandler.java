package com.wilutions.itol;

import java.util.ResourceBundle;

import com.wilutions.itol.db.Property;

public class PropertyTableViewHandler {
	
	public static Row createRow(Property prop, ResourceBundle resb) {
		String localizedName = resb.getString(prop.getId());
		return new Row(localizedName, prop);
	}
	
	public static class Row {
		
		private String localizedName;
		
		private Property property;
		
		public Row(String localizedName, Property property) {
			this.localizedName = localizedName;
			this.property = property;
		}
		
		public String getLocalizedName() {
			return localizedName;
		}

		public void setLocalizedName(String localizedName) {
			this.localizedName = localizedName;
		}

		public Property getProperty() {
			return property;
		}

		public void setProperty(Property property) {
			this.property = property;
		}
		
		
	}
}
