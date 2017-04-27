/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.dict.dlg.DlgFactory;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.RefreshAction;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.io.Serializable;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 *
 * @author Andrew Samsonov
 */
public abstract class EditableDictionary<T extends Serializable> extends GridForm {

  public EditableDictionary(String title) {
    super(title);
  }

  public EditableDictionary(String title, boolean delayLoad) {
    super(title, delayLoad);
  }

  protected Action editAction(final EditableDialog editableDlg, final String failureMessage, final String errorMessage, final String successMessage) {
    return new GridAction(grid, null, editableDlg.getCaption(), new Image(ImageConstants.INSTANCE.edit24()), 10, true) {

      @Override
      public void execute() {
        Row row = grid.getCurrentRow();
        if (row == null) {
          return;
        }
        editableDlg.setDlgEvents(this);
        editableDlg.clearContent();
        editableDlg.show(row);
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, FormAction.UPDATE, editableDlg, failureMessage, errorMessage, successMessage);
      }
    };
  }

  protected Action createAction(final EditableDialog editableDlg, final String failureMessage, final String errorMessage, final String successMessage) {
    return new GridAction(grid, null, editableDlg.getCaption(), new Image(ImageConstants.INSTANCE.new24()), 10) {

      @Override
      public void execute() {
        editableDlg.setDlgEvents(this);
        editableDlg.clearContent();
        editableDlg.show();
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, FormAction.CREATE, editableDlg, failureMessage, errorMessage, successMessage);
      }
    };
  }


  protected Action createActionWithParams(final EditableDialog editableDlg, final String failureMessage, final String errorMessage, final String successMessage) {
    return new GridAction(grid, null, editableDlg.getCaption(), new Image(ImageConstants.INSTANCE.new24()), 10) {
      @Override
      public void execute() {
        Row row = grid.getCurrentRow();
        if (row == null) {
          return;
        }

        editableDlg.setDlgEvents(this);
        editableDlg.clearContent();
        editableDlg.show(row);
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, FormAction.CREATE, editableDlg, failureMessage, errorMessage, successMessage);
      }
    };
  }

  protected Action deleteAction(final EditableDialog editableDlg, final String failureMessage, final String errorMessage, final String successMessage) {
    return new GridAction(grid, null, editableDlg.getCaption(), new Image(ImageConstants.INSTANCE.stop()), 10, true) {

      @Override
      public void execute() {
        Row row = grid.getCurrentRow();
        if (row == null) {
          return;
        }
        editableDlg.setDlgEvents(this);
        editableDlg.clearContent();
        editableDlg.show(row);
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, FormAction.DELETE, editableDlg, failureMessage, errorMessage, successMessage);
      }
    };
  }

  protected Action otherAction(final EditableDialog editableDlg, final String failureMessage, final String errorMessage, final String successMessage,
                               ImageResource imageResource) {
    return new GridAction(grid, null, editableDlg.getCaption(), new Image(imageResource), 10, true) {

      @Override
      public void execute() {
        Row row = grid.getCurrentRow();
        if (row == null) {
          return;
        }
        editableDlg.setDlgEvents(this);
        editableDlg.clearContent();
        editableDlg.show(row);
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, FormAction.OTHER, editableDlg, failureMessage, errorMessage, successMessage);
      }
    };
  }

  public void onAction(T cnw, FormAction action, EditableDialog editableDlg, String failureMessage, String errorMessage, String successMessage) throws Exception {
    WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
    save(cnw, action, new AsyncCallbackImpl<T>(editableDlg, refreshAction, action, failureMessage, errorMessage, successMessage));
  }

  protected abstract void save(T cnw, FormAction action, AsyncCallback<RpcRes_Base<T>> asyncCallbackImpl) throws Exception;

  protected String getSuccessMessage(T wrapper, FormAction action){
    return null;
  }

  private class AsyncCallbackImpl<U extends Serializable> extends AuthCheckAsyncCallback<RpcRes_Base<U>> {

    private final DlgFrame dlg;
    protected RefreshAction refreshAction;
    protected FormAction action;
    private final String failureMessage;
    private final String errorMessage;
    private final String successMessage;

    public AsyncCallbackImpl(DlgFrame dlg, RefreshAction refreshAction, FormAction action, String failureMessage, String errorMessage, String successMessage) {
      this.dlg = dlg;
      this.refreshAction = refreshAction;
      this.action = action;
      this.failureMessage = failureMessage;
      this.errorMessage = errorMessage;
      this.successMessage = successMessage;
    }

    @Override
    public void onFailureOthers(Throwable caught) {
      WaitingManager.hide();
      showInfo(failureMessage, caught.getLocalizedMessage());
    }

    @Override
    public void onSuccess(RpcRes_Base<U> result) {
      if (result.isError()) {
        showInfo(errorMessage, result.getMessage());
      } else {
        //Window.alert(successMessage + result.getResult().getCode());
        String message = EditableDictionary.this.getSuccessMessage((T) result.getResult(), action);
        if (isEmpty(message))
          showInfo(successMessage);
        else
          showInfo(successMessage, message);
        dlg.hide();
        refreshAction.execute();
      }

      WaitingManager.hide();
    }
  }



  //Lazy creation dialog
  private HashMap<String, EditableDialog> cache = new HashMap<>();
  protected Object[] getInitParams(){ return null;}
  protected Action commonLazyAction(final String clazz,
                                    final String caption,
                                    final FormAction action,
                                    final Columns columns,
                                    final String failureMessage,
                                    final String errorMessage,
                                    final String successMessage) {

    ImageResource res = null;
    boolean checkHasRow = true;
    switch (action) {
      case CREATE:
        res = ImageConstants.INSTANCE.new24();
        checkHasRow = false;
        break;
      case UPDATE:
        res = ImageConstants.INSTANCE.edit24();
        break;
      case DELETE:
        res = ImageConstants.INSTANCE.stop();
        break;
    }

    //System.out.println("Create action " + action.name());
    return new GridAction(grid, null, caption, new Image(res), 10, checkHasRow) {
      EditableDialog dlg = null;
      @Override
      public void execute() {
        Row row = grid.getCurrentRow();
        if (action != FormAction.CREATE && row == null) {
          return;
        }
       // System.out.println("Type=" + clazz.getSimpleName());
        dlg = cache.get(clazz);
        if (dlg == null) {
          dlg = DlgFactory.create(clazz);
          if (dlg == null) {
        	  Window.alert("Класс '" + clazz + "' не создан!");
        	  return;
          }
          dlg.setIntiParams(getInitParams());
          cache.put(clazz, dlg);
         // System.out.println("Create dlg for " + action.name());
        }
        dlg.setCaption(caption);
        dlg.setFormAction(action);
        dlg.setColumns(columns);
        dlg.setDlgEvents(this);
        dlg.clearContent();

        dlg.show(row);
       // System.out.println("Show dlg for " + action.name());
      }

      @Override
      public void onDlgOkClick(Object prms) throws Exception {
        onAction((T) prms, action, dlg, failureMessage, errorMessage, successMessage);
      }
    };
  }
}
