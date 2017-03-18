package com.wilutions.itol.db;

/**
 * Item for attachment blacklist.
 * Attachments with this size and hash (MD5) value should not be added to an issue.
 * The name is an arbitrary String that can help to remember, which file should not be added.
 * An example for a blacklist item is a company logo.
 */
public class AttachmentBlacklistItem {
	
	private String name;
	private long size;
	private String hash;
	private transient boolean deleted;
	
	public AttachmentBlacklistItem() {
		name = "";
		size = 0;
		hash = "";
	}
	
	public AttachmentBlacklistItem(String name, long size, String hash) {
		this.name = name;
		this.size = size;
		this.hash = hash;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}

	@Override
	public String toString() {
		return "[name=" + name + ", size=" + size + ", hash=" + hash + "]";
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	
}
