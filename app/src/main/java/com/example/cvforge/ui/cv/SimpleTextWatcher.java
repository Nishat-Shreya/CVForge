package com.example.cvforge.ui.cv;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/** Calls a listener with the new text after every change in an EditText. */
public class SimpleTextWatcher implements TextWatcher {

    public interface OnTextChanged {
        void onChanged(String text);
    }

    private final OnTextChanged listener;

    private SimpleTextWatcher(OnTextChanged listener) {
        this.listener = listener;
    }

    public static void watch(EditText editText, OnTextChanged listener) {
        editText.addTextChangedListener(new SimpleTextWatcher(listener));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        listener.onChanged(s.toString());
    }
}
