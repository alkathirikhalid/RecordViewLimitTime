package com.devlomi.record_view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class RecordLockView extends RelativeLayout {

    private View mLayoutLock;
    private View mImageViewLockArrow;
    private View mImageViewLock;

    public RecordLockView(Context context) {
        super(context);
        init(context);
    }

    public RecordLockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecordLockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = View.inflate(context, R.layout.layout_record_lock_view, null);
        addView(view);

        mLayoutLock = view.findViewById(R.id.layoutLock);
        mImageViewLock = view.findViewById(R.id.imageViewLock);
        mImageViewLockArrow = view.findViewById(R.id.imageViewLockArrow);
    }

    public View getmLayoutLock() {
        return mLayoutLock;
    }

    public View getmImageViewLockArrow() {
        return mImageViewLockArrow;
    }

    public View getmImageViewLock() {
        return mImageViewLock;
    }
}


