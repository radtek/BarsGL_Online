package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.AbstractDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.shared.SqlQueryTimeoutException;
import ru.rbt.shared.Assert;

import java.util.List;

import static ru.rbt.barsgl.shared.SqlQueryTimeoutException.SQL_TIMEOUT_MESSAGE;

/**
 * Created by akichigi on 02.04.15.
 */


public abstract class GridDataProvider extends AbstractDataProvider<Row> {

    private Range range;
    private String gridId;
    private IProviderEvents events;
    private boolean delayLoad;
    private OnfailureCallback onfailureCountCallback = new DefaultOnfailureCallback();
    private OnfailureCallback onfailureRowsCallback = new DefaultOnfailureCallback();

    protected GridDataProvider() {
        this(false);
    }

    protected GridDataProvider(OnfailureCallback onfailureCountCallback, OnfailureCallback onfailureRowsCallback) {
        this(false);
        this.onfailureCountCallback = Assert.notNull(onfailureCountCallback, "count failure callback is null");
        this.onfailureRowsCallback = Assert.notNull(onfailureRowsCallback, "rows failure callback is null");
    }

    protected GridDataProvider(boolean delayLoad) {
        super(null);
        this.delayLoad = delayLoad;
    }


    public void activate() {
        delayLoad = false;
    }

    @Override
    protected void onRangeChanged(HasData<Row> display) {
        if (delayLoad) return;
        WaitingManager.show("Обновление данных...");
        range = display.getVisibleRange();
        getServerCount(callbackCount());
    }

    protected abstract void getServerCount(AsyncCallback<Integer> callback);
    protected abstract void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback);

    private AsyncCallback<Integer> callbackCount() {
        return new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                updateRowCount(Math.abs(result), result >= 0);
                getServerData(range.getStart(), range.getLength(), callbackRows());
            }
            @Override
            public void onFailure(Throwable caught) {
                try {
                    onfailureCountCallback.onfailure(caught);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        };
    }

    private AsyncCallback<List<Row>> callbackRows() {
        return new AsyncCallback<List<Row>>() {
            @Override
            public void onSuccess(List<Row> result) {
                updateRowData(range.getStart(), result);
                if (events != null) events.onDataLoad();
                WaitingManager.hide();
            }
            @Override
            public void onFailure(Throwable caught) {
                try {
                    onfailureRowsCallback.onfailure(caught);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        };
    }

    public void setGridId(String id) {
        gridId = id;
    }

    public void setEvents(IProviderEvents events){
        this.events = events;
    }

    private class DefaultOnfailureCallback implements OnfailureCallback {
        @Override
        public void onfailure(Throwable t) throws Throwable {
            if (isSqlQueryTimeoutException(t)) {
                DialogManager.error("Ошибка", ((SqlQueryTimeoutException)t).getUserMessage());
                if (WaitingManager.isWaiting()) {
                    WaitingManager.hide();
                }
            }
            else
                throw t;
        }
    }

    public static boolean isSqlQueryTimeoutException(Throwable throwable) {
        return (null != throwable && throwable.getLocalizedMessage().contains(SQL_TIMEOUT_MESSAGE));
//                || throwable instanceof SqlQueryTimeoutException;
    }

}

