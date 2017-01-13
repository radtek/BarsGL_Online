package ru.rbt.barsgl.gwt.core.widgets;

/**
 * Created by akichigi on 27.04.15.
 */

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;


public class GridPager extends SimplePager {

    // Page size is normally derieved from the visibleRange
    private int pageSize;

    public GridPager(int pageSize){
    	super(SimplePager.TextLocation.CENTER, true, true);
        this.pageSize = pageSize;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        super.setPageSize(pageSize);
    }

    // We want pageSize to remain constant
    @Override
    public int getPageSize() {
        return pageSize;
    }

    // Page forward by an exact size rather than the number of visible
    // rows as is in the norm in the underlying implementation
    @Override
    public void nextPage() {
        if (getDisplay() != null) {
            Range range = getDisplay().getVisibleRange();
            setPageStart(range.getStart() + getPageSize());
        }
    }

    // Page back by an exact size rather than the number of visible rows
    // as is in the norm in the underlying implementation
    @Override
    public void previousPage() {
        if (getDisplay() != null) {
            Range range = getDisplay().getVisibleRange();
            setPageStart(range.getStart() - getPageSize());
        }
    }

    // Override so the last page is shown with a number of rows less
    // than the pageSize rather than always showing the pageSize number
    // of rows and possibly repeating rows on the last and penultimate
    // page
    @Override
    public void setPageStart(int index) {
        if (getDisplay() != null) {
            Range range = getDisplay().getVisibleRange();
            int displayPageSize = getPageSize();
            if (isRangeLimited() && getDisplay().isRowCountExact()) {
                displayPageSize = Math.min(getPageSize(), getDisplay()
                        .getRowCount() - index);
            }
            index = Math.max(0, index);
            if (index != range.getStart()) {
                getDisplay().setVisibleRange(index, displayPageSize);
            }
        }
    }

    // Override to display "0 of 0" when there are no records (otherwise
    // you get "1-1 of 0") and "1 of 1" when there is only one record
    // (otherwise you get "1-1 of 1"). Not internationalised (but
    // neither is SimplePager)
    protected String createText() {
        NumberFormat formatter = NumberFormat.getFormat("#,###");
        HasRows display = getDisplay();
        Range range = display.getVisibleRange();
        int pageStart = range.getStart() + 1;
        int pageSize = range.getLength();
        int dataSize = display.getRowCount();
        int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
        endIndex = Math.max(pageStart, endIndex);
        boolean exact = display.isRowCountExact();
        if (dataSize == 0) {
            return "0 из 0";
        } else if (pageStart == endIndex) {
            return formatter.format(pageStart) + " из "
                    + formatter.format(dataSize);
        }
        return formatter.format(pageStart) + "-" + formatter.format(endIndex)
                + (exact ? " из " : " из свыше ") + formatter.format(dataSize);
    }

    /*@Override
    public void setPage(int index){
        Window.alert("index = " + index);
        HasRows display = getDisplay();
        Window.alert("display = " + display != null ? "not null" : "null");
        Window.alert("isRangeLimited = " + isRangeLimited() + "\nhasPage = " + hasPage(index));

        if( display != null && (!isRangeLimited() || !display.isRowCountExact() || hasPage(index))) {

            int pageSize = getPageSize();
            Window.alert("pageSize=" +  getPageSize());
            display.setVisibleRange(pageSize * index, pageSize);
        }
    }*/
}
