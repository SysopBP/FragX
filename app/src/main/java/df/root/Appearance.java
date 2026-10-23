package df.root;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import df.root.databinding.ActivityMainBinding;

/** Local color presets, applied without recreating an activity during a root run. */
final class Appearance {
    private static final String[] NAMES = {
            "Silver", "Ocean", "Mint", "Forest", "Amber", "Rose", "Wine", "Lavender"};
    private static final int[] COLORS = {
            0xFFD4D4D4, 0xFF9CCAFF, 0xFF8DE0CF, 0xFFA9D799,
            0xFFF4CE8A, 0xFFF4AEC3, 0xFFD9A0BA, 0xFFC9B3F5};

    private static int index(Context context) {
        int saved = context.getSharedPreferences("fragx_appearance", Context.MODE_PRIVATE)
                .getInt("palette", 0);
        return Math.max(0, Math.min(COLORS.length - 1, saved));
    }

    static int accent(Context context) { return COLORS[index(context)]; }

    static int surface(Context context) {
        int c = accent(context);
        return Color.argb(185, Color.red(c) / 5, Color.green(c) / 5, Color.blue(c) / 5);
    }

    static void choose(Context context, ActivityMainBinding binding) {
        CharSequence[] labels = new CharSequence[NAMES.length];
        for (int i = 0; i < labels.length; i++) {
            SpannableString label = new SpannableString("●   " + NAMES[i]);
            label.setSpan(new ForegroundColorSpan(COLORS[i]), 0, 1, 0);
            labels[i] = label;
        }
        GlassDialog.show(new AlertDialog.Builder(context).setTitle("Color theme")
                .setSingleChoiceItems(labels, index(context), (dialog, selected) -> {
                    context.getSharedPreferences("fragx_appearance", Context.MODE_PRIVATE)
                            .edit().putInt("palette", selected).apply();
                    apply(context, binding);
                    dialog.dismiss();
                }).setNegativeButton("Cancel", null).create());
    }

    static GradientDrawable glass(Context context, float radius) {
        int c = accent(context);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(225, Color.red(c)/3, Color.green(c)/3, Color.blue(c)/3),
                        Color.argb(220, Color.red(c)/10, Color.green(c)/10, Color.blue(c)/10)});
        d.setCornerRadius(radius * context.getResources().getDisplayMetrics().density);
        d.setStroke(Math.max(1, Math.round(context.getResources().getDisplayMetrics().density)),
                (c & 0x00FFFFFF) | 0x66000000);
        return d;
    }

    static void apply(Context context, ActivityMainBinding binding) {
        updateChildren(binding.getRoot());
        int c = accent(context);
        binding.getRoot().setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(Color.red(c)/12, Color.green(c)/12, Color.blue(c)/12), Color.BLACK}));
        if (context instanceof android.app.Activity) {
            android.view.Window w = ((android.app.Activity) context).getWindow();
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.BLACK);
        }
        binding.twoStep.invalidate();
        binding.floatingActions.setBackground(glass(context, 38));
        binding.odinPill.setBackground(glass(context, 28));
        binding.btnMenu.setBackground(glass(context, 28));
        if (binding.btnRun.isEnabled()) {
            binding.btnRun.setBackgroundTintList(ColorStateList.valueOf(accent(context)));
            binding.btnRun.setTextColor(0xFF111722);
        }
        if (binding.btnKsu.isEnabled())
            binding.btnKsu.setBackgroundTintList(ColorStateList.valueOf(accent(context)));
    }

    private static void updateChildren(View view) {
        if (view instanceof DraggableCardView) ((DraggableCardView) view).refreshPalette();
        if (view instanceof OneUiSwitch) ((OneUiSwitch) view).refreshPalette();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) updateChildren(group.getChildAt(i));
        }
    }
}
