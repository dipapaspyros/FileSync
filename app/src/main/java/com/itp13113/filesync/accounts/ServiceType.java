package com.itp13113.filesync.accounts;

public class ServiceType {
	public String id;
	public String title;
	public String icon;
	public String auth_type;
	public String listUri;
	
	ServiceType(String id, String title, String icon, String auth_type, String listUri) {
		this.id = id;
		this.title = title;
		this.icon = icon;
		this.auth_type = auth_type;
		this.listUri = listUri;
	}
	
	public String toString() {
		return this.title + ": ic:" + this.icon + " auth:" + this.auth_type + " ls:" + this.listUri;
	}
}
