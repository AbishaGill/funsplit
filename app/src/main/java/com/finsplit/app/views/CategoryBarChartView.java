package com.finsplit.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.finsplit.app.models.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryBarChartView extends View {

    private static final int ROW_H_DP   = 48;
    private static final int LABEL_W_DP = 76;
    private static final int AMOUNT_W_DP = 76;
    private static final int BAR_H_DP   = 10;
    private static final int GAP_DP     = 8;

    private final List<Entry> entries = new ArrayList<>();
    private float density;

    private final Paint trackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CategoryBarChartView(Context context) {
        super(context);
        init();
    }

    public CategoryBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryBarChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        trackPaint.setColor(0xFFE8DDD8);
        labelPaint.setColor(0xFF554339);
        amountPaint.setColor(0xFF1D1B19);
        labelPaint.setTextSize(13 * density);
        amountPaint.setTextSize(12 * density);
    }

    public void setData(Map<String, Double> totals) {
        entries.clear();
        if (totals == null || totals.isEmpty()) {
            requestLayout();
            invalidate();
            return;
        }
        double max = 0;
        for (double v : totals.values()) max = Math.max(max, v);
        if (max == 0) return;

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(totals.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Double> e : sorted) {
            if (e.getValue() > 0) {
                Category cat = Category.fromString(e.getKey());
                entries.add(new Entry(cat.label(), e.getValue(), e.getValue() / max, colorFor(cat)));
            }
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = (int)(entries.size() * ROW_H_DP * density);
        setMeasuredDimension(w, Math.max(h, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (entries.isEmpty()) return;

        int rowH    = (int)(ROW_H_DP   * density);
        int labelW  = (int)(LABEL_W_DP  * density);
        int amountW = (int)(AMOUNT_W_DP * density);
        int barH    = (int)(BAR_H_DP   * density);
        int gap     = (int)(GAP_DP     * density);
        float radius = barH / 2f;

        float barAreaLeft  = labelW;
        float barAreaRight = getWidth() - amountW - gap;

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            float cy = i * rowH + rowH / 2f;

            // Label (vertically centered)
            Paint.FontMetrics fm = labelPaint.getFontMetrics();
            float textY = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(e.label, 0, textY, labelPaint);

            // Track background
            float top    = cy - barH / 2f;
            float bottom = cy + barH / 2f;
            canvas.drawRoundRect(barAreaLeft, top, barAreaRight, bottom, radius, radius, trackPaint);

            // Colored fill
            float fillRight = barAreaLeft + (float)(e.ratio * (barAreaRight - barAreaLeft));
            if (fillRight > barAreaLeft) {
                barPaint.setColor(e.color);
                canvas.drawRoundRect(barAreaLeft, top, fillRight, bottom, radius, radius, barPaint);
            }

            // Amount label
            String amtText = String.format(Locale.getDefault(), "₨%,.0f", e.amount);
            Paint.FontMetrics afm = amountPaint.getFontMetrics();
            float aty = cy - (afm.ascent + afm.descent) / 2f;
            canvas.drawText(amtText, barAreaRight + gap, aty, amountPaint);
        }
    }

    private static int colorFor(Category cat) {
        switch (cat) {
            case FOOD:      return 0xFFE07B39;
            case TRANSPORT: return 0xFF265EAA;
            case RENT:      return 0xFF9A4601;
            case UTILITIES: return 0xFF006C44;
            default:        return 0xFF85736B;
        }
    }

    private static class Entry {
        final String label;
        final double amount;
        final double ratio;
        final int color;
        Entry(String l, double a, double r, int c) {
            label = l; amount = a; ratio = r; color = c;
        }
    }
}
