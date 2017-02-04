/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.Serializable;

import javafx.scene.image.Image;

public class IdName implements Serializable {
	
	private static final long serialVersionUID = 5993942030314036816L;
	
	public static final IdName NULL = new IdName();
	
	private String id;
	
	private String name;
	
	private transient Image image;

	public IdName(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public IdName(String idname) {
		this.id = idname;
		this.name = idname;
	}

	public IdName(int id, String name) {
		this.id = String.valueOf(id);
		this.name = name;
	}

	public IdName(String id, String name, Image image) {
		this.id = id;
		this.name = name;
		this.image = image;
	}

	public IdName() {
		this.id = "";
		this.name = "";
	}

	public IdName(IdName rhs) {
		this.id = rhs.id;
		this.name = rhs.name;
	}

	public String toString() {
		return this.name;
	}
	
	public boolean isNull() {
		return this == NULL;
	}
	
	public String getId() {
		if (id == null) return "";
		return id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Image getImage() {
		return image;
	}
	
	public void setImage(Image image) {
		this.image = image;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IdName))
			return false;
		IdName other = (IdName) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	
}
