package com.noober.menu;


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.InflateException;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.graphics.drawable.GradientDrawable.RECTANGLE;

/**
 * Created by xiaoqi on 2017/12/11.
 */

public class FloatMenu extends PopupWindow{

	/** Menu tag name in XML. */
	private static final String XML_MENU = "menu";

	/** Group tag name in XML. */
	private static final String XML_GROUP = "group";

	/** Item tag name in XML. */
	private static final String XML_ITEM = "item";

	private static final int ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;


	private final int DEFAULT_MENU_WIDTH;
	private final int VERTICAL_OFFSET;

	private Context context;
	private List<String> menuItemList;
	private View view;
	private Point screenPoint;
	private int clickX;
	private int clickY;
	private int menuWidth;
	private int menuHeight;
	private LinearLayout menuLayout;
	private OnItemClickListener onItemClickListener;


	public interface OnItemClickListener {
		void onClick(View v, int position);
	}

	public FloatMenu(Activity activity){
		this(activity, activity.findViewById(android.R.id.content));
	}

	public FloatMenu(Context context, View view) {
		super(context);
		setOutsideTouchable(true);
		setFocusable(true);
		setBackgroundDrawable(new BitmapDrawable());
		view.setOnTouchListener(new MenuTouchListener());
		this.context = context;
		this.view = view;
		VERTICAL_OFFSET = Display.dip2px(context, 10);
		DEFAULT_MENU_WIDTH = Display.dip2px(context, 180);
		screenPoint = Display.getScreenMetrics(context);
		menuItemList = new ArrayList<>();
	}

	public void inflate(int menuRes) {
		inflate(menuRes, DEFAULT_MENU_WIDTH);
	}

	public void inflate(int menuRes, int itemWidth) {
		XmlResourceParser parser = null;
		try {
			parser = context.getResources().getLayout(menuRes);
			AttributeSet attrs = Xml.asAttributeSet(parser);
			parseMenu(parser, attrs);
		} catch (XmlPullParserException e) {
			throw new InflateException("Error inflating menu XML", e);
		} catch (IOException e) {
			throw new InflateException("Error inflating menu XML", e);
		} finally {
			if (parser != null) parser.close();
		}
		generateLayout(itemWidth);
	}

	public void items(String... items) {
		items(DEFAULT_MENU_WIDTH, items);
	}

	public void items(int itemWidth, String... items) {
		menuItemList = Arrays.asList(items);
		generateLayout(itemWidth);
	}

	private void generateLayout(int itemWidth) {
		menuLayout = new LinearLayout(context);
//		menuLayout.setBackgroundColor(Color.WHITE);
//		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//			menuLayout.setOutlineProvider(new ViewOutlineProvider() {
//				@Override
//				public void getOutline(View view, Outline outline) {
//					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//						outline.setRect(0, 0, view.getWidth(), view.getHeight());
//					}
//				}
//			});
//			menuLayout.setElevation(Display.dip2px(context, 10));
//			menuLayout.setTranslationZ(Display.dip2px(context, 10));
//		}
//		menuLayout.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_shadow));
		menuLayout.setOrientation(LinearLayout.VERTICAL);

		int padding = Display.dip2px(context, 12);
		for(int i = 0; i < menuItemList.size(); i ++){
			TextView textView = new TextView(context);
			textView.setClickable(true);
			textView.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.selector_item));
			textView.setPadding(padding, padding, padding, padding);
			textView.setWidth(itemWidth);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
			textView.setTextSize(15);
			textView.setTextColor(Color.BLACK);
			textView.setText(menuItemList.get(i));
			if(onItemClickListener != null){
				textView.setOnClickListener(new ItemOnClickListener(i));
			}
			menuLayout.addView(textView);
		}
		int width = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
		int height =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
		menuLayout.measure(width,height);
		menuWidth = menuLayout.getMeasuredWidth();
		menuHeight = menuLayout.getMeasuredHeight();

		int shadowWidth = Display.dip2px(context, 10);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
				menuWidth + shadowWidth, menuHeight + shadowWidth);
		menuLayout.setPadding(shadowWidth / 2,shadowWidth / 2,shadowWidth / 2,shadowWidth / 2);
		menuLayout.setLayoutParams(layoutParams);
		GradientDrawable drawable = new GradientDrawable();
		drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		drawable.setGradientRadius((float) Math.sqrt(layoutParams.width * layoutParams.width + layoutParams.height * layoutParams.height)
				/2);
		drawable.setGradientCenter(0.5f, 0.5f);
		drawable.setShape(GradientDrawable.RECTANGLE);
		drawable.setOrientation(GradientDrawable.Orientation.BR_TL);
		int[] colors = new int[3];
		colors[0] = Color.parseColor("#00969696");
		colors[1] = Color.parseColor("#7EDEDEDE");
		colors[2] = Color.parseColor("#00FFFFFF");
		drawable.setColors(colors);
		menuLayout.setBackground(drawable);
		setContentView(menuLayout);
		setWidth(layoutParams.width);
		setHeight(layoutParams.height);

	}

	private void parseMenu(XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
		int eventType = parser.getEventType();
		String tagName;
		boolean lookingForEndOfUnknownTag = false;
		String unknownTagName = null;

		// This loop will skip to the menu start tag
		do {
			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (tagName.equals(XML_MENU)) {
					// Go to next tag
					eventType = parser.next();
					break;
				}
				throw new RuntimeException("Expecting menu, got " + tagName);
			}
			eventType = parser.next();
		} while (eventType != XmlPullParser.END_DOCUMENT);

		boolean reachedEndOfMenu = false;
		while (!reachedEndOfMenu) {
			switch (eventType) {
				case XmlPullParser.START_TAG:
					if (lookingForEndOfUnknownTag) {
						break;
					}
					tagName = parser.getName();
					if (tagName.equals(XML_GROUP)) {
					//	parser group
						parser.next();
					} else if (tagName.equals(XML_ITEM)) {
						readItem(attrs);
					} else if (tagName.equals(XML_MENU)) {
						// A menu start tag denotes a submenu for an item
						//pares subMenu
						parser.next();
					} else {
						lookingForEndOfUnknownTag = true;
						unknownTagName = tagName;
					}
					break;

				case XmlPullParser.END_TAG:
					tagName = parser.getName();
					if (lookingForEndOfUnknownTag && tagName.equals(unknownTagName)) {
						lookingForEndOfUnknownTag = false;
						unknownTagName = null;
					} else if (tagName.equals(XML_GROUP)) {

					} else if (tagName.equals(XML_ITEM)) {

					} else if (tagName.equals(XML_MENU)) {
						reachedEndOfMenu = true;
					}
					break;

				case XmlPullParser.END_DOCUMENT:
					throw new RuntimeException("Unexpected end of document");
			}

			eventType = parser.next();
		}
	}

	private void readItem(AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MenuItem);
		CharSequence itemTitle = a.getText(R.styleable.MenuItem_menu_title);
//		int itemIconResId = a.getResourceId(R.styleable.MenuItem_icon, 0);
		menuItemList.add(String.valueOf(itemTitle));
		a.recycle();
	}

	public void show(Point point){
		clickX = point.x;
		clickY = point.y;
		show();
	}

	public void show(){
		if(isShowing()){
			return;
		}
		//it is must ,other wise 'setOutsideTouchable' will not work under Android5.0
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
			setBackgroundDrawable(new BitmapDrawable());
		}

		if(clickX <= screenPoint.x / 2){
			if(clickY + menuHeight < screenPoint.y){
				setAnimationStyle(R.style.Animation_top_left);
				showAtLocation(view, ANCHORED_GRAVITY, clickX , clickY + VERTICAL_OFFSET);
			}else {
				setAnimationStyle(R.style.Animation_bottom_left);
				showAtLocation(view, ANCHORED_GRAVITY, clickX , clickY - menuHeight - VERTICAL_OFFSET);
			}
		}else {
			if(clickY + menuHeight < screenPoint.y){
				setAnimationStyle(R.style.Animation_top_right);
				showAtLocation(view, ANCHORED_GRAVITY, clickX - menuWidth , clickY + VERTICAL_OFFSET);
			}else {
				setAnimationStyle(R.style.Animation_bottom_right);
				showAtLocation(view, ANCHORED_GRAVITY, clickX - menuWidth, clickY - menuHeight - VERTICAL_OFFSET);
			}
		}

	}


	@Override
	public void setOnDismissListener(OnDismissListener onDismissListener) {
		super.setOnDismissListener(onDismissListener);
	}

	public void setOnItemClickListener(final OnItemClickListener onItemClickListener){
		this.onItemClickListener = onItemClickListener;
		if(onItemClickListener != null){
			for (int i = 0; i < menuLayout.getChildCount(); i ++){
				View view = menuLayout.getChildAt(i);
				view.setOnClickListener(new ItemOnClickListener(i));
			}
		}
	}

	class MenuTouchListener implements View.OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				clickX = (int) event.getRawX();
				clickY = (int) event.getRawY();
			}
			return false;
		}
	}

	class ItemOnClickListener implements View.OnClickListener {
		int position;

		public ItemOnClickListener(int position){
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			dismiss();
			if(onItemClickListener != null){
				onItemClickListener.onClick(v, position);
			}
		}
	}
}
