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
import ru.rbt.barsgl.gwt.client.check.CheckFileExtention;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.core.comp.Components;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.RichAreaBox;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

import static ru.rbt.barsgl.gwt.core.comp.Components.createAlignWidget;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by er18837 on 17.10.2018.
 */
public abstract class LoadFileAnyDlg extends DlgFrame implements IAfterShowEvent {
    private final static String LIST_DELIMITER = "#";

    private final String BORDER_WIDTH = "120px";
    private final String LABEL_WIDTH = "110px";
    private final String FIELD_WIDTH = "300px";

    private static final String UPLOAD_TYPE = "uploadtype";

    protected FormPanel formPanel;
    protected FileUpload fileUpload;
    protected Anchor anchorExample;

    protected Button errorButton;
    protected Button showButton;
    protected Button uploadButton;
    protected Button deleteButton;
    protected Panel loadingResult;

    protected HorizontalPanel hidden;
    protected Hidden fileName;

    private RichAreaBox requestBox;

    private Timer timer;

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

    protected abstract boolean acceptResponse(String[] list);
    protected abstract void onClickDelete(ClickEvent clickEvent);
    protected abstract void onClickUpload(ClickEvent clickEvent);
    protected abstract void onClickShow(ClickEvent clickEvent);
    protected abstract void onClickError(ClickEvent clickEvent);

    protected abstract void switchControlsState(Boolean state);

    @Override
    public Widget createContent(){
        return createUploadPanelBase();
    }

    protected Panel createUploadPanelBase() {
        VerticalPanel panel = new VerticalPanel();

        panel.add(new Hidden(UPLOAD_TYPE, getUploadType()));

        hidden = new HorizontalPanel();
        fileName = new Hidden();
        fileName.setName("filename");
        hidden.add(fileName);
        panel.add(fileName);

        Label selectLabel = Components.createLabel("Файл для загрузки");
        panel.add(selectLabel);

        fileUpload = new FileUpload();
        fileUpload.setName(getFileUploadName());
        fileUpload.setWidth("500px");
        panel.add(fileUpload);

        Grid g2 = new Grid(1, 4);
        g2.setWidget(0, 0, uploadButton = createUploadButton());
        g2.setWidget(0, 1, errorButton = createErrorButton());
        g2.setWidget(0, 2, showButton = createShowButton());
        g2.setWidget(0, 3, deleteButton = createDeleteButton());
        errorButton.setEnabled(false);
        deleteButton.setEnabled(false);
        showButton.setEnabled(false);
        panel.add(g2);

        loadingResult = new VerticalPanel();
        loadingResult.setWidth("100%");
        panel.add(loadingResult);

        formPanel = new FormPanel();
        formPanel.setWidget(panel);
        formPanel.setAction(getServletUploadName());
        formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        formPanel.setMethod(FormPanel.METHOD_POST);

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

        panel.add(hp);

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

        fileUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                loadingResult.clear();
            }
        });

        fileUpload.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                loadingResult.clear();
            }
        });

        panel.setSpacing(10);

        return formPanel;
    }

    private String parseResponse(String response) {
        try {
            errorButton.setEnabled(false);
            showButton.setEnabled(false);
            deleteButton.setEnabled(false);
            if (isEmpty(response))
                return response;
            if (response.contains("NotAuthorizedUserException")) {
                SecurityEntryPoint.showLoginForm();
                return null;
            }
            if (response.startsWith(LIST_DELIMITER)) {
                String[] list = response.split(LIST_DELIMITER);

/*
                idPackage = parseLong(list[1], "пакет", ":");
                Long all = parseLong(list[2], "всего", ":");
                Long err = parseLong(list[3], "с ошибкой", ":");
                boolean isError = (null != err) && (err > 0);
                errorButton.setEnabled(isError);
                showButton.setEnabled(idPackage != null);
                deleteButton.setEnabled(idPackage != null);
                boolean isOk = !(idPackage == null || isError);
*/
                boolean isOk = acceptResponse(list);
                ok.setEnabled(isOk);
                switchControlsState(!isOk);

                return new StringBuilder()
                        .append(list[1]).append("<BR>")
                        .append(list[2]).append("<BR>")
                        .append(list[3]).toString();
            } else
                return response.replaceAll(LIST_DELIMITER, "<BR>");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    protected Long parseLong(String stringWithNumber, String delim) {
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
                    errorButton.setEnabled(false);
                    showButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    ok.setEnabled(false);
                    loadingResult.clear();

                    onClickUpload(event);
/*
                    idPackage = null;
                    CheckNotEmptyString check = new CheckNotEmptyString();
                    source.setValue(check((String) mSource.getValue()
                            , "Источник сделки", "поле не заполнено", check));
                    department.setValue((String) mDepartment.getValue());
                    movementOff.setValue(excludeOper.getValue().toString());
*/

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

    @Override
    public void afterShow() {
        switchControlsState(true);
        errorButton.setEnabled(false);
        showButton.setEnabled(false);
        deleteButton.setEnabled(false);
        ok.setEnabled(false);
        loadingResult.clear();
    }
}
