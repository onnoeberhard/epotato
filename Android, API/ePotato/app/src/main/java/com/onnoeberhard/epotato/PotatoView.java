package com.onnoeberhard.epotato;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PotatoView extends RelativeLayout {

    static final ArrayList<Integer> potatoForms = new ArrayList<>(
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 38, 40,
                    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52));
    private static final Map<Integer, int[]> forms = new HashMap<Integer, int[]>() {{
        put(1, new int[]{R.drawable.potato1, 330, 460, -40, 10});
        put(2, new int[]{R.drawable.potato2, 340, 460, 20, 15});
        put(3, new int[]{R.drawable.potato3, 370, 460, 20, 20});
        put(4, new int[]{R.drawable.potato4, 390, 460, -70, 0});
        put(5, new int[]{R.drawable.potato5, 390, 460, -20, 10});
        put(6, new int[]{R.drawable.potato6, 360, 460, -20, -10});
        put(7, new int[]{R.drawable.potato7, 350, 485, -90, 20});
        put(8, new int[]{R.drawable.potato8, 250, 430, 70, 0});
        put(9, new int[]{R.drawable.potato9, 210, 450, 20, 15});
        put(10, new int[]{R.drawable.potato10, 210, 450, -15, 0});
        put(11, new int[]{R.drawable.potato11, 180, 400, 0, -30});
        put(12, new int[]{R.drawable.potato12, 180, 310, 0, 10});
        put(13, new int[]{R.drawable.potato13, 300, 400, -30, -20});
        put(14, new int[]{R.drawable.potato14, 300, 400, -30, 0});
        put(15, new int[]{R.drawable.potato15, 300, 400, -30, 0});
        put(16, new int[]{R.drawable.potato16, 350, 390, 70, 0});
        put(17, new int[]{R.drawable.potato17, 270, 490, -20, -10});
        put(18, new int[]{R.drawable.potato18, 350, 420, 50, -10});
        put(19, new int[]{R.drawable.potato19, 350, 410, 70, 10});
        put(20, new int[]{R.drawable.potato20, 350, 410, -80, 0});
        put(21, new int[]{R.drawable.potato21, 300, 480, -70, 15});
        put(22, new int[]{R.drawable.potato22, 300, 480, -70, 0});
        put(23, new int[]{R.drawable.potato23, 270, 500, 10, 0});
        put(24, new int[]{R.drawable.potato24, 250, 480, 0, -15});
        put(25, new int[]{R.drawable.potato25, 250, 430, -20, 50});
        put(26, new int[]{R.drawable.potato26, 280, 430, 30, 10});
        put(27, new int[]{R.drawable.potato27, 380, 440, 70, 20});
        put(28, new int[]{R.drawable.potato28, 350, 460, -50, 30});
        put(29, new int[]{R.drawable.potato29, 350, 470, -20, 10});
        put(30, new int[]{R.drawable.potato30, 350, 470, 40, 15});
        put(31, new int[]{R.drawable.potato31, 350, 470, -20, -10});
        put(32, new int[]{R.drawable.potato32, 340, 460, 10, -10});
        put(33, new int[]{R.drawable.potato33, 340, 470, 10, 20});
        put(34, new int[]{R.drawable.potato34, 340, 460, 20, 20});
        put(35, new int[]{R.drawable.potato35, 270, 460, -50, 15});
        put(36, new int[]{R.drawable.potato36, 270, 460, 30, 20});
        put(37, new int[]{R.drawable.potato37, 240, 460, -10, -10});
        put(38, new int[]{R.drawable.potato38, 250, 460, 0, -10});
        put(39, new int[]{R.drawable.potato39, 300, 390, 20, 5});
        put(40, new int[]{R.drawable.potato40, 300, 390, -20, 10});
        put(41, new int[]{R.drawable.potato41, 270, 470, 30, 20});
        put(42, new int[]{R.drawable.potato42, 270, 400, 30, -30});
        put(43, new int[]{R.drawable.potato43, 340, 400, -90, 0});
        put(44, new int[]{R.drawable.potato44, 340, 450, 70, 5});
        put(45, new int[]{R.drawable.potato45, 280, 510, 0, 0});
        put(46, new int[]{R.drawable.potato46, 280, 480, 0, -30});
        put(47, new int[]{R.drawable.potato47, 280, 380, -30, 0});
        put(48, new int[]{R.drawable.potato48, 280, 380, 40, 0});
        put(49, new int[]{R.drawable.potato49, 280, 380, -40, 0});
        put(50, new int[]{R.drawable.potato50, 200, 300, 0, 0});
        put(51, new int[]{R.drawable.potato51, 330, 440, -40, 0});
        put(52, new int[]{R.drawable.potato52, 360, 400, 70, 0});
    }};
    boolean hr = false;
    Callback callback;
    private ImageView iv;
    private int maxwidth = 320;
    private int width = 1024;
    private int height = 1024;
    private String text;
    private int form;
    private boolean singleLine = false;
    private Canvas c;
    private Paint p;
    private Bitmap potato;
    private Bitmap b;
    private Context context;
    private boolean asked = false;
    private View visItem;
    private boolean maxed = false;

    public PotatoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.v_potato, this);
        iv = (ImageView) findViewById(R.id.iv);
        iv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (maxed) layout();
            }
        });
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PotatoView);
            text = ta.getString(R.styleable.PotatoView_text) == null ? "" : ta.getString(R.styleable.PotatoView_text);
            form = ta.getInt(R.styleable.PotatoView_form, 0) == 0 ? potatoForms.get(new Random().nextInt(potatoForms.size())) : ta.getInt(R.styleable.PotatoView_form, 0);
            maxwidth = ta.getInt(R.styleable.PotatoView_maxwidth, maxwidth);
            singleLine = ta.getBoolean(R.styleable.PotatoView_singleLine, singleLine);
            ta.recycle();
            setup(form, text, null);
        }
//        setup(form, "\u2588", null);
    }

    public String getText() {
        return text;
    }

    public void setText(String _text) {
        text = _text;
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        c.drawBitmap(potato, new Rect(0, 0, potato.getWidth(), potato.getHeight()), new Rect(0, 0, width, height), p);
        p.setColor(Color.BLACK);
        p.setTextSize(180);
        p.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/patrickhand_regular.ttf"));
        StaticLayout sl = new StaticLayout(text, new TextPaint(p), width - forms.get(form)[1], Layout.Alignment.ALIGN_CENTER, .8f, 0, false);
        while (sl.getHeight() > forms.get(form)[2] || singleLine && sl.getLineCount() > 1) {
            p.setTextSize(p.getTextSize() - 1);
            sl = new StaticLayout(sl.getText(), new TextPaint(p), width - forms.get(form)[1], Layout.Alignment.ALIGN_CENTER, .8f, 0, false);
        }
        c.save();
        c.translate(((float) width - (float) sl.getWidth()) / 2f + forms.get(form)[3], ((float) height - (float) sl.getHeight()) / 2f + forms.get(form)[4]);
        sl.draw(c);
        c.restore();
    }

    public int getForm() {
        return form;
    }

    public void setup(int _form, final String _text, Callback callback) {
        this.callback = callback;
        new Painter().execute(Integer.toString(_form), _text);
    }

    public void setVisItem(View visItem) {
        this.visItem = visItem;
    }

    private void layout() {
        if (iv != null && iv.getHeight() > 0) {
            RelativeLayout.LayoutParams lp = null;
            if (!maxed) {
                maxed = true;
                int w = (int) (maxwidth * getResources().getDisplayMetrics().density);
                lp = new RelativeLayout.LayoutParams(w, w * height / width);
            } else if ((float) iv.getHeight() / (float) iv.getWidth() > (float) height / (float) width) {
                maxed = false;
                lp = new RelativeLayout.LayoutParams(iv.getWidth(), iv.getWidth() * height / width);
            } else if ((float) iv.getHeight() / (float) iv.getWidth() < (float) height / (float) width) {
                maxed = false;
                lp = new RelativeLayout.LayoutParams(iv.getHeight() * width / height, iv.getHeight());
            }
            if (lp != null) {
                lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                iv.setLayoutParams(lp);
                requestLayout();
            }
        } else new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                layout();
            }
        }, 50);
    }

    Bitmap bitmapWithWatermark() {
        Bitmap start = ((BitmapDrawable) iv.getDrawable()).getBitmap();
        Bitmap b = Bitmap.createBitmap(width + 20, height + 80, Bitmap.Config.ARGB_8888);
        Canvas _c = new Canvas(b);
        Paint _p = new Paint();
        _p.setAntiAlias(true);
        _c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        _c.drawBitmap(start, new Rect(0, 0, start.getWidth(), start.getHeight()), new Rect(10, 10, width + 10, height + 10), _p);
        _p.setColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDarkTransparent, null));
        _p.setTextSize(80);
        _p.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/patrickhand_regular.ttf"));
        StaticLayout sl = new StaticLayout(getContext().getString(R.string.app_name), new TextPaint(_p), width, Layout.Alignment.ALIGN_OPPOSITE, .8f, 0, false);
        _c.save();
        _c.translate(0, height - 10);
        sl.draw(_c);
        _c.restore();
        return b;
    }

    String saveToGallery(boolean showNotice, final Activity a) {
        String result = "-";
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(a, Manifest.permission.WRITE_EXTERNAL_STORAGE) && showNotice) {
                new AlertDialog.Builder(a).setTitle(R.string.saveImage)
                        .setMessage(R.string.storageNotice)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                saveToGallery(false, a);
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                saveToGallery(false, a);
                            }
                        }).show();
            } else
                ActivityCompat.requestPermissions(a, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, SendPotatoActivity.STORAGE_PERMISSION);
            asked = true;
        } else {
            try {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ePotatoes/";
                File dir = new File(path);
                if (dir.exists() || dir.mkdirs()) {
                    String file = path + LocalDatabaseHandler.getTimestamp() + ".png";
                    FileOutputStream out = new FileOutputStream(file);
                    bitmapWithWatermark().compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                    result = asked ? FIIDService.ALLOW : FIIDService.OK;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    interface Callback {
        void callback();
    }

    private class Painter extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            form = Integer.parseInt(params[0]);
            width = 1024;
            potato = BitmapFactory.decodeResource(getResources(), forms.get(form)[0]);
            height = (int) ((float) potato.getHeight() / (float) potato.getWidth() * (float) width);
            b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            c = new Canvas(b);
            p = new Paint();
            p.setAntiAlias(true);
            setText(params[1]);
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            iv.setImageBitmap(b);
            if (visItem != null)
                visItem.setVisibility(View.VISIBLE);
            maxed = false;
            layout();
            if (callback != null) callback.callback();
            callback = null;
        }
    }

}
