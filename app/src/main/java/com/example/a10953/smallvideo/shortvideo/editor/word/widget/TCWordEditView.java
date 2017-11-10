package com.example.a10953.smallvideo.shortvideo.editor.word.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.a10953.smallvideo.R;
import com.example.a10953.smallvideo.shortvideo.editor.Edit;

/**
 * Created by hanszhli on 2017/6/16.
 */

public class TCWordEditView extends RelativeLayout {
    private Edit.OnWordChangeListener mListener;
    private RelativeLayout mAddWordBtn;

    public TCWordEditView(Context context) {
        super(context);
        init();
    }

    public TCWordEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TCWordEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View.inflate(getContext(), R.layout.item_word_edit_view, this);
        mAddWordBtn = (RelativeLayout) findViewById(R.id.word_rl_add);
        mAddWordBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onWordClick();
                }
            }
        });
    }

    public void setIWordEventListener(Edit.OnWordChangeListener listener) {
        mListener = listener;
    }

    public void setOnClickable(boolean onClickable) {
        mAddWordBtn.setClickable(onClickable);
    }
}
