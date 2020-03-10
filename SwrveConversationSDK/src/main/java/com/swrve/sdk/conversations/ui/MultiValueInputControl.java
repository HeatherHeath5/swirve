package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.swrve.sdk.conversations.R;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.ConversationInputChangedListener;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MultiValueInputControl extends LinearLayout implements Serializable, IConversationInput, OnCheckedChangeListener {

    private MultiValueInput model;
    private int selectedIndex = -1; // default to none selected
    private ConversationInputChangedListener inputChangedListener;
    private TextView titleTextView;
    private ArrayList<RadioButton> radioButtons;

    @SuppressLint("NewApi")
    public MultiValueInputControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MultiValueInputControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiValueInputControl(Context context) {
        super(context);
    }

    /*
     * Inflates but does not add to parent container. Caller will need to add it.
     */
    public static MultiValueInputControl inflate(Context context, ViewGroup parentContainer, MultiValueInput model) {
        LayoutInflater layoutInf = LayoutInflater.from(context);
        MultiValueInputControl control = (MultiValueInputControl) layoutInf.inflate(R.layout.swrve__multiinput, parentContainer, false);
        control.titleTextView = control.findViewById(R.id.swrve__MIV_Header);
        control.titleTextView.setText(model.getDescription());
        ConversationStyle titleStyle = model.getStyle();
        int titleTextColorInt =  titleStyle.getTextColorInt();
        SwrveConversationHelper.setBackgroundDrawable(control, titleStyle.getBg().getPrimaryDrawable());
        control.titleTextView.setTextColor(titleTextColorInt);
        control.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, titleStyle.getTextSize());
        control.titleTextView.setTypeface(titleStyle.getTypeface());
        int padding = context.getResources().getDimensionPixelSize(R.dimen.swrve__conversation_mvi_padding);
        control.titleTextView.setPadding(padding, padding, padding, padding);

        control.model = model;
        control.radioButtons = new ArrayList<>();

        for (int i = 0; i < model.getValues().size(); i++) {
            RadioButton rb = new RadioButton(context);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rb.setLayoutParams(lp);
            ChoiceInputItem item = model.getValues().get(i);
            rb.setText(item.getAnswerText());
            rb.setTypeface(item.getStyle().getTypeface());
            rb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, item.getStyle().getTextSize());
            rb.setTextColor(item.getStyle().getTextColorInt());
            MultiValueInputControl.setTint(rb, item.getStyle().getTextColorInt());

            rb.setChecked(i == control.selectedIndex);
            if (!control.isInEditMode()) {
                rb.setTag(R.string.swrve__indexTag, i);
            }

            control.addView(rb);
            rb.setOnCheckedChangeListener(control);
            control.radioButtons.add(rb);
        }
        return control;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setTint(RadioButton radioButton, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            radioButton.setButtonTintList(ColorStateList.valueOf(color));
        } else {
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] {-android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed}  // pressed
            };
            int[] colors = new int[]{color, color, color, color};
            ColorStateList colorStateList = new ColorStateList(states, colors);
            Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(radioButton.getContext(), R.drawable.abc_btn_radio_material));
            DrawableCompat.setTintList(drawable, colorStateList);
            radioButton.setButtonDrawable(drawable);
        }
    }

    @Override
    public void setUserInput(UserInputResult userInput){
        ChoiceInputResponse choice = (ChoiceInputResponse) userInput.getResult();
        for(RadioButton rb : radioButtons){
            if (rb.getText().toString().equalsIgnoreCase(choice.getAnswerText()))
            {
                rb.setChecked(true);
            }
        }
    }

    private Map<String, Object> gatherValue() {
        Map<String, Object> dataMap = new HashMap<>();
        ChoiceInputItem mv = model.getValues().get(selectedIndex);
        ChoiceInputResponse r = new ChoiceInputResponse();
        r.setQuestionID(model.getTag());
        r.setFragmentTag(model.getTag());
        r.setAnswerID(mv.getAnswerID());
        r.setAnswerText(mv.getAnswerText());
        dataMap.put(model.getTag(), r);
        return dataMap;
    }

    public void setContentChangedListener(ConversationInputChangedListener inputChangedListener) {
        this.inputChangedListener = inputChangedListener;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int index = (Integer) buttonView.getTag(R.string.swrve__indexTag);

        if (selectedIndex > -1 && selectedIndex != index) {
            RadioButton oldChecked = (RadioButton) getChildAt(selectedIndex + 1);
            if (oldChecked.isChecked()) {
                oldChecked.setOnCheckedChangeListener(null); //not want to repeat myself
                oldChecked.setChecked(false);
                oldChecked.setOnCheckedChangeListener(this);
            }
        }

        if (!isChecked) {
            selectedIndex = -1;
        } else {
            selectedIndex = index;
        }
        if (inputChangedListener != null) {
            inputChangedListener.onContentChanged(gatherValue(), model);
        }
    }

    public ArrayList<RadioButton> getRadioButtons() {
        return this.radioButtons;
    }
}
