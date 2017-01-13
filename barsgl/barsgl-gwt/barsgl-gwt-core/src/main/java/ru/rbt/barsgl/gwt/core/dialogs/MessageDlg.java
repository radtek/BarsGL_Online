package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by ER18837 on 24.11.15.
 */
public abstract class MessageDlg extends DlgFrame {

//    private HTML text;
    private VerticalPanel vp;

    public MessageDlg() {
        super();
        setModal(true);
}

    @Override
    public Widget createContent() {
        vp = new VerticalPanel();
        vp.addStyleName("Table_Header");
        vp.addStyleName("FrameDecorLine");
        vp.setSpacing(5);
        return vp;
    }

    @Override
    protected void fillContent(){
        vp.clear();
    	ArrayList<String> list = new ArrayList<>();
    	list.addAll(Arrays.asList(((String) params).split("\n")));
    	for (String line : list) {
    		vp.add(new HTML(line));
    	}
    }

}
