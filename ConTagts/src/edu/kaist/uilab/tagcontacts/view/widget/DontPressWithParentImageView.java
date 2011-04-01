// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Special class to to allow the parent to be pressed without being pressed itself.
 */
public class DontPressWithParentImageView extends ImageView {

	public DontPressWithParentImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	 @Override
	public void setPressed(boolean pressed) {
		 // If the parent is pressed, do not set to pressed.
	   if (pressed && ((View) getParent()).isPressed()) {
	  	 return;
	   }
	   super.setPressed(pressed);
	}
}
