package com.onnoeberhard.potato;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class FontTextView extends AppCompatTextView {

    public static final String NORMAL = "normal";
    public static final String BOLD = "bold";

    public static final String BARIOL = "bariol";
    private static final String PATRICKHAND = "patrickhand";
    private static final String PATRICKHAND_SC = "patrickhandsc";

    private String font;
    private String style;

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FontTextView);
        style = ta.getString(R.styleable.FontTextView_textStyle) == null ? NORMAL : ta.getString(R.styleable.FontTextView_textStyle);
        font = ta.getString(R.styleable.FontTextView_font) == null ? BARIOL : ta.getString(R.styleable.FontTextView_font);
        ta.recycle();
        update();
    }

    public void setFont(String _font) {
        font = _font;
        update();
    }

    public void setStyle(String _style) {
        style = _style;
        update();
    }

    private void update() {
        switch (font) {
            case BARIOL:
                switch (style) {
                    case BOLD:
                        setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/bariol_bold.ttf"));
                        break;
                    default:
                        setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/bariol_regular.ttf"));
                }
                break;
            case PATRICKHAND:
                setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/patrickhand_regular.ttf"));
                break;
            case PATRICKHAND_SC:
                setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/patrickhand_sc.ttf"));
                break;
        }
    }

}
