// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.graphics.Color;

/**
 * Immutable class for representing the group entity.
 * 
 * <p> This group entity is different from {@link GroupEntity} in which it
 * is independent of any contact whereas a {@link GroupEntity} always belong
 * to a specific contact.
 * 
 * <p> A group contains a label (group name) and its associated color.
 *  
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Group {
	
	/**
	 * Default color (white) for group with no color.
	 */
	public static final int DEFAULT_COLOR = Color.WHITE;
	
	public static final long NO_GROUP_ID = -1L;
	
	private long id;
	private String label;
	private int color;

	public Group(long id, String label, int color) {
		this.id = id;
		this.label = label;
		this.color = color;
	}
	
	/**
	 * Constructor
	 * 
	 * @param label
	 * @param color
	 */
	public Group(String label, int color) {
		this(NO_GROUP_ID, label, color);
	}
	
	/**
	 * Creates an instance with the given label and the default color.
	 * 
	 * @param label
	 */
	public Group(String label) {
		this(NO_GROUP_ID, label, DEFAULT_COLOR);
	}
	
	public long getId() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getColor() {
		return color;
	}
}
