package ru.rbt.barsgl.gwt.client.loadFile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.apache.commons.lang3.ArrayUtils;
import ru.rbt.barsgl.gwt.client.check.CheckFileExtention;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.core.comp.Components;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.RichAreaBox;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

import java.sql.Array;
import java.util.Arrays;

import static ru.rbt.barsgl.gwt.core.comp.Components.createAlignWidget;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by er18837 on 17.10.2018.
 */
public abstract class LoadFileAnyDlg extends DlgFrame implements IAfterShowEvent {
    protected final static String LIST_DELIMITER = "#";

    protected final String LABEL_WIDTH = "130px";
    protected final String FIELD_WIDTH = "300px";

    protected static final String UPLOAD_TYPE = "uploadtype";

    protected FormPanel formPanel;
    protected FileUpload fileUpload;
    protected Anchor anchorExample;

    protected Button errorButton;
    protected Button showButton;
    protected Button uploadButton;
    protected Button deleteButton;
    protected Panel loadingResult;

    protected Panel hidden;
    protected Hidden fileName;

    private RichAreaBox requestBox;

    public LoadFileAnyDlg(){
        super();
        ok.setText("Сохранить");
        ok.setEnabled(false);
        setAfterShowEvent(this);
    }

    protected abstract String getFileUploadName();
    protected abstract String getServletUploadName();

    protected abstract String getUploadType();
    protected abstract String getExampleName();

    protected abstract void onClickDelete(ClickEvent clickEvent);
    protected abstract void onClickUpload(ClickEvent clickEvent);
    protected abstract void onClickShow(ClickEvent clickEvent);
    protected abstract void onClickError(ClickEvent clickEvent);

    protected abstract boolean acceptResponse(String[] list);

    @Override
    public Widget createContent(){
        VerticalPanel panel = new VerticalPanel();

        createHiddenPanel(panel);
        createFileUpload(panel);
        createButtons(panel);
        createResult(panel);
        createExample(panel);
        formPanel = createFormPanel(panel);

        panel.setSpacing(10);

        return formPanel;
    }

    protected Panel createHiddenPanel(Panel parentPanel) {
        parentPanel.add(new Hidden(UPLOAD_TYPE, getUploadType()));
        hidden = new HorizontalPanel();
        fileName = new Hidden();
        fileName.setName("filename");
        hidden.add(fileName);

        parentPanel.add(fileName);
        return hidden;

    }

    protected FileUpload createFileUpload(Panel parentPanel) {
        Label selectLabel = Components.createLabel("Файл для загрузки");
        parentPanel.add(selectLabel);

        fileUpload = new FileUpload();
        fileUpload.setName(getFileUploadName());
        fileUpload.setWidth("100%");    // 500px
        parentPanel.add(fileUpload);

        fileUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                switchControlsState(true);
                switchButtonState(false, false);
                loadingResult.clear();
            }
        });

        fileUpload.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                loadingResult.clear();
            }
        });

        return fileUpload;
    }

    protected Panel createButtons(Panel parentPanel) {
        Panel g2 = new HorizontalPanel();
        g2.setWidth("100%");
        g2.add(createAlignWidget(uploadButton = createUploadButton(), LABEL_WIDTH));
        g2.add(createAlignWidget(errorButton = createErrorButton(), LABEL_WIDTH));
        g2.add(createAlignWidget(showButton = createShowButton(), LABEL_WIDTH));
        g2.add(createAlignWidget(deleteButton = createDeleteButton(), LABEL_WIDTH));

        errorButton.setEnabled(false);
        deleteButton.setEnabled(false);
        showButton.setEnabled(false);

        parentPanel.add(g2);
        return g2;
    }

    protected Panel createResult(Panel parentPanel) {
        loadingResult = new VerticalPanel();
        loadingResult.setWidth("100%");
        parentPanel.add(loadingResult);
        return loadingResult;
    }

    protected Panel createExample(Panel parentPanel) {
        final String urlExample = GWT.getHostPageBaseURL() + "excel/" + getExampleName();
        HorizontalPanel hp = new HorizontalPanel();
        hp.add(new Label("Скачать пример "));
        anchorExample = new Anchor(getExampleName());
        anchorExample.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
        anchorExample.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.open(urlExample, "_self", "disabled");
            }
        });
        hp.add(anchorExample);

        parentPanel.add(hp);
        return hp;
    }

    protected FormPanel createFormPanel(Panel parentPanel) {
        formPanel = new FormPanel();
        formPanel.setWidget(parentPanel);
        formPanel.setAction(getServletUploadName());
        formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        formPanel.setMethod(FormPanel.METHOD_POST);

        formPanel.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                switchControlsState(true);
                loadingResult.clear();
                String message = parseResponse(event.getResults());
                if (!isEmpty(message)) {
                    requestBox = new RichAreaBox();
                    requestBox.setReadOnly(true);
                    requestBox.setWidth("100%");
                    requestBox.setHeight("200px");
                    requestBox.setHTML(message.replaceAll(LIST_DELIMITER, "<BR>"));
                    loadingResult.add(requestBox);
                }
            }
        });
        return formPanel;
    }

    private String parseResponse(String response) {
        try {
            switchButtonState(false, false);
            if (isEmpty(response))
                return response;
            if (response.contains("NotAuthorizedUserException")) {
                SecurityEntryPoint.showLoginForm();
                return null;
            }
            if (response.startsWith(LIST_DELIMITER)) {
                String[] list = response.split(LIST_DELIMITER);
                boolean isOk = acceptResponse(list);

                ok.setEnabled(isOk);
                switchControlsState(!isOk);
            }
            return response.replaceAll(LIST_DELIMITER, "<BR>");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    protected Long parseLong(String stringWithNumber, String text, String delim) {
        int index;
        if (!isEmpty(stringWithNumber) && (index = stringWithNumber.indexOf(delim)) >= 0) {
            try {
                return Long.decode(stringWithNumber.substring(index+1).trim());
            } catch (Exception e) {
                loadingResult.add(new HTML(e.getMessage()));
            }
        }
        return null;
    }

    private Button createDeleteButton() {
        Button btn = new Button("Удаление пакета", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onClickDelete(clickEvent);
            }
        });
        btn.addStyleName("dlg-button");
        return btn;
    }

    private Button createUploadButton() {
        Button btn = new Button();
        btn.setText("Загрузка");
        btn.addStyleName("dlg-button");
        btn.setWidth(LABEL_WIDTH);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                //get the filename to be uploaded
                try {
                    switchControlsState(false);
                    switchButtonState(false, false);
                    loadingResult.clear();

                    onClickUpload(event);

                    String filename = check(fileUpload.getFilename(), "Файл для загрузки", "не выбран", new CheckNotEmptyString());

                    fileName.setValue(check(filename, "Файл для загрузки", "нужен файл типа 'xlsx'", new CheckFileExtention("xlsx")));

                    loadingResult.add(new Label("Ожидайте, идет загрузка проводок из файла ..."));
                    formPanel.submit();

                } catch (IllegalArgumentException e) {
                    switchControlsState(true);
                    if (e.getMessage() == null || !e.getMessage().equals("column")) {
                        throw e;
                    }
                }
            }
        });
        return btn;
    }

    private Button createShowButton() {
        Button btn = new Button();
        btn.setText("Просмотр пакета");
        btn.addStyleName("dlg-button");

        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onClickShow(clickEvent);
            }
        });
        return btn;
    }

    private Button createErrorButton() {
        Button btn = new Button();
        btn.setText("Ошибки загрузки");
        btn.addStyleName("dlg-button");

        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onClickError(clickEvent);
            }
        });
        return btn;
    }

    protected void switchControlsState(boolean state) {
        uploadButton.setEnabled(state);
        anchorExample.setEnabled(state);
    }

    protected void switchButtonState(boolean state, boolean error) {
        errorButton.setEnabled(error);
        showButton.setEnabled(state);
        deleteButton.setEnabled(state);
        ok.setEnabled(state && !error);
    }

    @Override
    public void afterShow() {
        switchControlsState(true);
        switchButtonState(false, false);
        loadingResult.clear();
    }
}
