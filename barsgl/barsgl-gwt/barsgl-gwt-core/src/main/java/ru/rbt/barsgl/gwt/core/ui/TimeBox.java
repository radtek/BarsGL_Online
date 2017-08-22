package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;


/**
 * Created by er17503 on 21.08.2017.
 */
public class TimeBox extends Composite{
    private static final String timePatternMask = "([0-1]?\\d|2[0-3]):([0-5]?\\d)";
    private TxtBox hourBox;

    private TxtBox minuteBox;
    private final String _hour = "00";
    private final String _minute = "00";

    private enum Direction {UP, DOWN}
    private enum TimePart {HOUR, MINUTE}
    private boolean isReadOnly;

    public TimeBox(){
       this("");
    }

    public TimeBox(String time){
        initWidget(configure());

        if (parseTimeStr(time)){
            normalizeTime(time);
        } else {
            clear();
        }
    }

    public TimeBox(int hour, int minute) {
       this(hour + ":" + minute);
    }

    private Widget configure(){
        hourBox = new TxtBox(){
            @Override
            protected boolean validate(String text) {
                hourBox.setValue(correctTime(TimePart.HOUR, text));
                return true;
            }
        };
        hourBox.setWidth("15px");
        hourBox.setMask("[0-9]");
        hourBox.setMaxLength(2);

        hourBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (isReadOnly) return;
                int keyCode = event.getNativeEvent().getKeyCode();
                switch (keyCode) {
                    case KeyCodes.KEY_UP:
                        hourBox.setValue(changeTime(TimePart.HOUR, hourBox.getText(), Direction.UP));
                        break;
                    case KeyCodes.KEY_DOWN:
                        hourBox.setValue(changeTime(TimePart.HOUR, hourBox.getText(), Direction.DOWN));
                        break;

                }
            }
        });

        minuteBox = new TxtBox(){
            @Override
            protected boolean validate(String text) {
                minuteBox.setValue(correctTime(TimePart.MINUTE, text));
                return true;
            }
        };
        minuteBox.setWidth("15px");
        minuteBox.setMask("[0-9]");
        minuteBox.setMaxLength(2);

        minuteBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (isReadOnly) return;
                int keyCode = event.getNativeEvent().getKeyCode();

                switch (keyCode) {
                    case KeyCodes.KEY_UP:
                        minuteBox.setValue(changeTime(TimePart.MINUTE, minuteBox.getText(), Direction.UP));
                        break;
                    case KeyCodes.KEY_DOWN:
                        minuteBox.setValue(changeTime(TimePart.MINUTE, minuteBox.getText(), Direction.DOWN));
                        break;
                }
            }
        });

        HorizontalPanel timePanel = new HorizontalPanel();
        timePanel.add(hourBox);
        timePanel.add(new Label(":"));
        timePanel.add(minuteBox);

        return timePanel;
    }

    private boolean parseTimeStr(String time){
        RegExp p = RegExp.compile(timePatternMask);
        return p.test(time);
    }

    private void normalizeTime(String time){
       String[] parts = time.split(":");
       hourBox.setValue(formatPart(parts[0]));
       minuteBox.setValue(formatPart(parts[1]));
    }

    private String formatPart(String part){
        return  part.length() == 1 ? "0" + part : part;
    }

    private String changeTime(TimePart part, String value, Direction dir){
        int val;
        try{
             val = Integer.parseInt(value);
        } catch (NumberFormatException ex){
            val = 0;
        }

        if (dir == Direction.UP) {
            val++;
        } else{
            val--;
        }

        int limit = part == TimePart.HOUR ? 23 : 59;

        if (val < 0 ) val = limit;
        else if (val > limit) val = 0;

        return formatPart(((Integer)val).toString());
    }

    private String correctTime(TimePart part, String value){
        int val;
        try{
            val = Integer.parseInt(value);
        } catch (NumberFormatException ex){
            val = 0;
        }

        int limit = part == TimePart.HOUR ? 23 : 59;

        if (val < 0 ) val = 0;
        else if (val > limit) val = limit;

        return formatPart(((Integer)val).toString());
    }

    @Override
    public String toString() {
        return  hourBox.getValue() + ":" + minuteBox.getValue();
    }

    public String getHourStr(){
        return hourBox.getValue();
    }

    public String getMinuteStr(){
        return minuteBox.getValue();
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
        hourBox.setReadOnly(isReadOnly);
        minuteBox.setReadOnly(isReadOnly);
    }

    public void setTime(String time){
        if (parseTimeStr(time)){
            normalizeTime(time);
        } else {
            clear();
        }
    }

    public void setTime(int hour, int minute){
        setTime(hour + ":" + minute);
    }

    public void setHour(String hour){
        hourBox.setValue(correctTime(TimePart.HOUR, hour));
    }

    public void setMinute(String minute){
        minuteBox.setValue(correctTime(TimePart.MINUTE, minute));
    }

    public void clear(){
        hourBox.setValue(_hour);
        minuteBox.setValue(_minute);
    }
}
