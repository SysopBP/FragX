package df.root;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

/** Consistent rounded panels for the theme picker and wallpaper controls. */
final class GlassDialog {
    static void show(AlertDialog dialog) {
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        Context context = dialog.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int accent = Appearance.accent(context);
        GradientDrawable panel = Appearance.glass(context, 28);
        panel.setAlpha(255);
        panel.setCornerRadius(28 * density);
        panel.setStroke(Math.max(1, Math.round(density)), (accent & 0xFFFFFF) | 0x66000000);
        window.setBackgroundDrawable(panel);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.35f;
        window.setAttributes(params);
        View decor = window.getDecorView();
        decor.setClipToOutline(true);
        View content = window.findViewById(android.R.id.content);
        if (content != null) style(content, accent);
        // Explicit side margins in portrait and a bounded width in landscape/tablets.
        android.graphics.Rect frame = new android.graphics.Rect();
        decor.getWindowVisibleDisplayFrame(frame);
        int availableWidth = frame.width() > 0 ? frame.width()
                : context.getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(Math.round(420 * density),
                availableWidth - Math.round(48 * density));
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        decor.post(() -> {
            if (!dialog.isShowing()) return;
            int screenHeight = frame.height() > 0 ? frame.height()
                    : context.getResources().getDisplayMetrics().heightPixels;
            int maxHeight = Math.round(screenHeight * 0.82f);
            if (decor.getHeight() > maxHeight) window.setLayout(width, maxHeight);
        });
        for (int button : new int[]{AlertDialog.BUTTON_POSITIVE,
                AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            TextView view = dialog.getButton(button);
            if (view != null) view.setTextColor(accent);
        }
    }

    private static void style(View view, int accent) {
        if (view instanceof ViewGroup) {
            // AlertController panels otherwise paint opaque square gray fills.
            view.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) style(group.getChildAt(i), accent);
        }
        if (view instanceof TextView) ((TextView) view).setTextColor(0xFFF1F4FA);
        if (view instanceof android.widget.CompoundButton)
            ((android.widget.CompoundButton) view).setButtonTintList(ColorStateList.valueOf(accent));
        if (view instanceof CheckedTextView)
            ((CheckedTextView) view).setCheckMarkTintList(ColorStateList.valueOf(accent));
        if (view instanceof SeekBar) {
            SeekBar bar = (SeekBar) view;
            bar.setThumbTintList(ColorStateList.valueOf(accent));
            bar.setProgressTintList(ColorStateList.valueOf(accent));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF596273));
        }
        if (view instanceof ListView) {
            ListView list = (ListView) view;
            list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override public void onChildViewAdded(View parent, View child) { style(child, accent); }
                @Override public void onChildViewRemoved(View parent, View child) { }
            });
        }
    }
}
