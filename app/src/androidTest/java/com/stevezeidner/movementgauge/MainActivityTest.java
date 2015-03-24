package com.stevezeidner.movementgauge;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity activity;
    private TextView textView;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
        textView = (TextView) activity.findViewById(R.id.gauge_title);
    }

    public void testLabelIsInterceptorOnLoad() {
        final String expected = activity.getString(R.string.instantaneous_title);
        final String actual = textView.getText().toString();
        assertEquals(expected, actual);
    }

    public void testReadWriteValueToSharedPrefs() {
        final float start = activity.readCumulative();
        final float expected = 1024.32f;
        activity.writeCumulative(expected);
        final float actual = activity.readCumulative();
        assertEquals(expected, actual);
        activity.writeCumulative(start);
    }


}