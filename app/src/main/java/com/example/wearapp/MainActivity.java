package com.example.wearapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

/** Билеты по вышмату (ДУ) на Wear OS: без WebView, только нативные View. */
public class MainActivity extends Activity {

    private static final int BG = 0xFF000000;
    private static final int CARD = 0xFF1E2430;
    private static final int ACCENT = 0xFF6EA8FE;

    private JSONArray tickets;
    private ScrollView scroll;
    private LinearLayout content;

    // стек экранов: {тип, билет, вопрос}
    private final ArrayList<int[]> stack = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFocusable(true);
        scroll.setFocusableInTouchMode(true);
        scroll.setVerticalScrollBarEnabled(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        scroll.requestFocus();

        try {
            tickets = new JSONArray(readAsset("tickets.json"));
            go(0, 0, 0, true);
        } catch (Throwable t) {
            content.removeAllViews();
            TextView tv = text("Ошибка загрузки:\n" + t, 13, Color.WHITE, Typeface.NORMAL);
            content.addView(tv);
        }
    }

    // ---------- навигация ----------

    private void go(int type, int ticket, int question, boolean doRender) {
        stack.add(new int[]{type, ticket, question});
        if (doRender) render();
    }

    private void render() {
        try {
            int[] s = stack.get(stack.size() - 1);
            content.removeAllViews();
            int w = getResources().getDisplayMetrics().widthPixels;
            // запас под круглый экран
            content.setPadding((int) (w * 0.10f), (int) (w * 0.16f), (int) (w * 0.10f), (int) (w * 0.26f));
            if (s[0] == 0) showMenu();
            else if (s[0] == 1) showTicket(s[1]);
            else showQuestion(s[1], s[2]);
            scroll.scrollTo(0, 0);
        } catch (Throwable t) {
            content.removeAllViews();
            content.addView(text("Ошибка:\n" + t, 13, Color.WHITE, Typeface.NORMAL));
        }
    }

    @Override
    public void onBackPressed() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
            render();
        } else {
            super.onBackPressed();
        }
    }

    // поворот колёсика / безеля
    @Override
    public boolean onGenericMotionEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_SCROLL
                && e.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float d = -e.getAxisValue(MotionEvent.AXIS_SCROLL);
            scroll.scrollBy(0, (int) (d * 80));
            return true;
        }
        return super.onGenericMotionEvent(e);
    }

    // ---------- экраны ----------

    private void showMenu() throws Exception {
        content.addView(text("Вышмат · ДУ", 17, ACCENT, Typeface.BOLD));
        content.addView(text("Выбери билет", 12, 0xFF9AA4B2, Typeface.NORMAL));
        int count = tickets.length();
        for (int i = 0; i < count; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int j = i; j < Math.min(i + 2, count); j++) {
                final int idx = j;
                int n = tickets.getJSONObject(j).getInt("n");
                TextView b = button("Билет " + n, 14, true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                b.setOnClickListener(v -> go(1, idx, 0, true));
                row.addView(b, lp);
            }
            content.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void showTicket(final int t) throws Exception {
        JSONObject ticket = tickets.getJSONObject(t);
        content.addView(text("Билет " + ticket.getInt("n"), 17, ACCENT, Typeface.BOLD));
        JSONArray qs = ticket.getJSONArray("questions");
        for (int i = 0; i < qs.length(); i++) {
            final int qi = i;
            JSONObject q = qs.getJSONObject(i);
            TextView b = button(q.getInt("num") + ". " + q.getString("title"), 13, false);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> go(2, t, qi, true));
            content.addView(b, matchWithMargin());
        }
        content.addView(backButton(), matchWithMargin());
    }

    private void showQuestion(int t, int qi) throws Exception {
        JSONObject q = tickets.getJSONObject(t).getJSONArray("questions").getJSONObject(qi);
        content.addView(text(Fmt.format(q.getInt("num") + ". " + q.getString("title")), 14, ACCENT, Typeface.BOLD));
        JSONArray blocks = q.getJSONArray("blocks");
        for (int i = 0; i < blocks.length(); i++) {
            JSONArray b = blocks.getJSONArray(i);
            String type = b.getString(0);
            String s = b.getString(1);
            TextView tv;
            switch (type) {
                case "h":
                    tv = text(Fmt.format(s), 14, Color.WHITE, Typeface.BOLD);
                    tv.setPadding(0, dp(8), 0, 0);
                    break;
                case "b":
                    tv = text(Fmt.format(s), 13, 0xFFC9D1DC, Typeface.BOLD);
                    break;
                case "f":
                case "s":
                    tv = text(Fmt.format(s), 14, 0xFFFFD27F, Typeface.NORMAL);
                    tv.setTypeface(Typeface.SERIF);
                    break;
                case "m":
                    tv = text(s.replace("'", "\u2032"), 11, 0xFFFFD27F, Typeface.NORMAL);
                    tv.setTypeface(Typeface.MONOSPACE);
                    break;
                case "ans":
                    tv = text(Fmt.format("Ответ: " + s), 14, 0xFF7DDC8B, Typeface.BOLD);
                    tv.setBackground(round(0xFF16301D, 10));
                    tv.setPadding(dp(8), dp(6), dp(8), dp(6));
                    break;
                default:
                    tv = text(Fmt.format(s), 13, 0xFFE6EAF0, Typeface.NORMAL);
            }
            tv.setGravity(Gravity.START);
            content.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        content.addView(backButton(), matchWithMargin());
    }

    // ---------- помощники UI ----------

    private TextView backButton() {
        TextView b = button("← Назад", 13, true);
        b.setOnClickListener(v -> onBackPressed());
        return b;
    }

    private LinearLayout.LayoutParams matchWithMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        return lp;
    }

    private TextView text(CharSequence s, float sp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setLineSpacing(0, 1.1f);
        tv.setPadding(0, dp(3), 0, dp(3));
        return tv;
    }

    private TextView button(String s, float sp, boolean center) {
        TextView b = text(s, sp, Color.WHITE, Typeface.BOLD);
        b.setGravity(center ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
        b.setBackground(round(CARD, 22));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private String readAsset(String name) throws Exception {
        try (InputStream is = getAssets().open(name)) {
            byte[] buf = new byte[is.available()];
            int off = 0;
            while (off < buf.length) {
                int r = is.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            return new String(buf, 0, off, "UTF-8");
        }
    }
}
