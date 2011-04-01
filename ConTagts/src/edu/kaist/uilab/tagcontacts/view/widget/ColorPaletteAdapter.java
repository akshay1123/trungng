// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import edu.kaist.uilab.tagcontacts.R;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Adapter for the color palette.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ColorPaletteAdapter extends BaseAdapter {
	private Context context;
	private int[] thumbIds = new int[] {
			R.drawable.color1, R.drawable.color2,
			R.drawable.color3, R.drawable.color4,
			R.drawable.color5, R.drawable.color6,
			R.drawable.color7, R.drawable.color8,
			R.drawable.color9, R.drawable.color10,
			R.drawable.color11, R.drawable.color12,
			R.drawable.color13, R.drawable.color14,
			R.drawable.color15, R.drawable.color16,
			R.drawable.color17, R.drawable.color18,
	};
	
	private int[] colors = new int[] {
		Color.argb(0xfa, 0xff, 0x48, 0x00),
		Color.argb(0xfa, 0xfe, 0x66, 0x0a),
		Color.argb(0xfa, 0xa7, 0xee, 0x00),
		Color.argb(0xff, 0x00, 0xfe, 0x00),
		Color.argb(0xff, 0x00, 0xaf, 0xf7),
		Color.argb(0xff, 0x82, 0xe2, 0xf0),
		Color.argb(0xff, 0xff, 0xff, 0x00),
		Color.argb(0xff, 0x97, 0xaa, 0x4f),
		Color.argb(0xff, 0x64, 0xaf, 0xa7),
		Color.argb(0xff, 0x33, 0xcc, 0xcc),
		Color.argb(0xff, 0x65, 0xbe, 0xb7),
		Color.argb(0xff, 0xc3, 0xc4, 0xbc),
		Color.argb(0xff, 0xf1, 0xd6, 0x77),
		Color.argb(0xff, 0x9d, 0xca, 0x38),
		Color.argb(0xff, 0xcd, 0x5c, 0xc6),
		Color.argb(0xff, 0xcd, 0x4c, 0xa0),
		Color.argb(0xff, 0x13, 0x66, 0xf9),
		Color.argb(0xff, 0xc1, 0xc5, 0x09),
	};
	
	public ColorPaletteAdapter(Context context) {
		this.context = context;
	}
	
	@Override
	public int getCount() {
		return thumbIds.length;
	}

	/**
	 * Returns the color underlying the item at given position.
	 */
	@Override
	public Object getItem(int position) {
		return new Integer(colors[position]);
	}

	@Override
	public long getItemId(int position) {
		// no id?
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) {
			imageView = new ImageView(context);
			imageView.setLayoutParams(new GridView.LayoutParams(40, 40));
		} else {
			// re-use this view (to avoid creating a new view)
			// the data can be changed by setImageResource later
			imageView = (ImageView) convertView;
		}
		
		imageView.setImageResource(thumbIds[position]);
		return imageView;
	}
}
