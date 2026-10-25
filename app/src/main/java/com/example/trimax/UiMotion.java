package com.example.trimax;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/** Small, shared motion touches used across screens. */
final class UiMotion {

    private UiMotion() {}

    /**
     * Counts a number up (or down) from what the view shows now to the new value.
     * Falls back to setting the text directly when the old text is not a number.
     */
    static void countTo(TextView view, int target) {
        if (view == null) return;
        int from;
        try {
            from = Integer.parseInt(view.getText().toString().trim());
        } catch (NumberFormatException e) {
            from = 0;
        }
        Object running = view.getTag(R.id.tag_count_animator);
        if (running instanceof ValueAnimator) ((ValueAnimator) running).cancel();
        if (from == target) {
            view.setText(String.valueOf(target));
            return;
        }
        ValueAnimator anim = ValueAnimator.ofInt(from, target);
        anim.setDuration(Math.min(900, 300 + Math.abs(target - from) * 40L));
        anim.setInterpolator(new DecelerateInterpolator(1.6f));
        anim.addUpdateListener(a -> view.setText(String.valueOf(a.getAnimatedValue())));
        view.setTag(R.id.tag_count_animator, anim);
        anim.start();
    }
}
