package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.check.CheckFileExtention;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.dictionary.BatchPostingFormDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.RichAreaBox;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by akichigi on 20.06.16.
 */
abstract public class LoadFileDlgBase extends DlgFrame {
    private final static String LIST_DELIMITER = "#";

    private final String LABEL_WIDTH = "110px";
    private final String FIELD_WIDTH = "300px";

    private static final String UPLOAD_TYPE = "uploadtype";

    private String caption;

    private FormPanel formPanel;
    private DataListBox mSource;
    private DataListBox mDepartment;
    private CheckBox excludeOper;
    private FileUpload fileUpload;
    private Anchor anchorExample;

    private Button errorButton;
    private Button showButton;
    private Button uploadButton;
    private Button deleteButton;
    private Panel loadingResult;

    private Hidden fileName;
    private Hidden source;
    private Hidden department;
    private Hidden movementOff;

    private RichAreaBox requestBox;
    private Long idPackage = null;

    private HandlerRegistration registration;

    public LoadFileDlgBase(String caption){
        super();
        registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, createDataListBoxEventHandler());
        this.caption = caption;
        setCaption(caption);
        ok.setText("Передать на подпись");
        ok.setEnabled(false);

    }

    protected abstract String getFileUploadName();
    protected abstract String getServletUploadName();
    protected abstract boolean isExcludeVisible();

    protected abstract String getUploadType();
    protected abstract String getExampleName();

    protected DataListBoxEventHandler createDataListBoxEventHandler(){
        return new DataListBoxEventHandler() {

            @Override
            public void completeLoadData(String dataListBoxId) {
                if (mDepartment.getId().equalsIgnoreCase(dataListBoxId)){
                    registration.removeHandler();
                    AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                    if (wrapper != null){
                        mDepartment.setValue(wrapper.getBranch());
                    }
                }
            }
        };
    }

    @Override
    public Widget createContent(){
        return createUploadPanel();
    }

    private Panel createUploadPanel() {
        VerticalPanel panel = new VerticalPanel();

        panel.add(new Hidden(UPLOAD_TYPE, getUploadType()));

        Grid g = new Grid(2, 2);
        panel.add(g);
        mSource = createDealSourceAuthListBox("", FIELD_WIDTH);
        g.setWidget(0, 0, createLabel("Источник сделки", LABEL_WIDTH));
        g.setWidget(0, 1, mSource);

        mDepartment = createDepartmentListBox("", FIELD_WIDTH, true);
        g.setWidget(1, 0, createLabel("Подразделение", LABEL_WIDTH));
        g.setWidget(1, 1, mDepartment);

        panel.add(excludeOper = new CheckBox("Исключение создания проводки в АБС по контролируемым счетам"));
        excludeOper.setVisible(isExcludeVisible());

        Label selectLabel = createLabel("Файл для загрузки");
        panel.add(selectLabel);

        fileUpload = new FileUpload();
        fileUpload.setName(getFileUploadName());
        fileUpload.setWidth("500px");
        panel.add(fileUpload);

        HorizontalPanel hidden = new HorizontalPanel();

        fileName = new Hidden();
        fileName.setName("filename");
        hidden.add(fileName);

        source = new Hidden();
        source.setName("source");
        hidden.add(source);

        department = new Hidden();
        department.setName("department");
        hidden.add(department);

        movementOff = new Hidden();
        movementOff.setName("movement_off");
        hidden.add(movementOff);

        panel.add(hidden);

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
                String message = parceResponce(event.getResults());
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

    private String parceResponce(String responce) {
        try {
            errorButton.setEnabled(false);
            showButton.setEnabled(false);
            deleteButton.setEnabled(false);
            if (isEmpty(responce))
                return responce;
            if (responce.contains("NotAuthorizedUserException")) {
                BarsGLEntryPoint.showLoginForm();
                return null;
            }
            if (responce.startsWith(LIST_DELIMITER)) {
                String[] list = responce.split(LIST_DELIMITER);
                idPackage = parseLong(list[1], "пакет", ":");
                Long all = parseLong(list[2], "всего", ":");
                Long err = parseLong(list[3], "с ошибкой", ":");
                boolean isError = (null != err) && (err > 0);
                errorButton.setEnabled(isError);
                showButton.setEnabled(idPackage != null);
                deleteButton.setEnabled(idPackage != null);
                ok.setEnabled(!(idPackage == null || isError));

                return new StringBuilder()
                        .append(list[1]).append("<BR>")
                        .append(list[2]).append("<BR>")
                        .append(list[3]).toString();
            } else
                return responce.replaceAll(LIST_DELIMITER, "<BR>");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private Long parseLong(String stringWithNumber, String keyWord, String delim) {
        int index = -1;
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
                ManualOperationWrapper wrapper = new ManualOperationWrapper();
                wrapper.setPkgId((Long) idPackage);
                wrapper.setAction(BatchPostAction.DELETE);

                AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                wrapper.setUserId(appUserWrapper.getId());

                BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
                        if (wrapper.isError()) {
                            showInfo("Ошибка", wrapper.getMessage());
                        } else {
                            deleteButton.setEnabled(false);
                            showButton.setEnabled(false);
                            errorButton.setEnabled(false);
                            loadingResult.clear();
                            idPackage = null;
                            showInfo("Информация", wrapper.getMessage());
                        }
                    }
                });
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
                    idPackage = null;
                    switchControlsState(false);
                    errorButton.setEnabled(false);
                    showButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    ok.setEnabled(false);
                    loadingResult.clear();

                    CheckNotEmptyString check = new CheckNotEmptyString();
                    source.setValue(check((String) mSource.getValue()
                            , "Источник сделки", "поле не заполнено", check));
                    department.setValue(check((String) mDepartment.getValue()
                            , "Подразделение", "поле не заполнено", check));
                    String filename = check(fileUpload.getFilename(), "Файл для загрузки", "не выбран", check);

                    fileName.setValue(check(filename, "Файл для загрузки", "нужен файл типа 'xlsx'", new CheckFileExtention("xlsx")));

                    movementOff.setValue(excludeOper.getValue().toString());
                    loadingResult.add(new Label("Ожидайте, идет загрузка проводок из файла ..."));
                    formPanel.submit();

                } catch (IllegalArgumentException e) {
                    if (e.getMessage() == null || !e.getMessage().equals("column")) {
                        throw e;
                    }
                } finally {
                    switchControlsState(true);
                }
            }
        });
        return btn;
    }

    private void switchControlsState(Boolean state){
        uploadButton.setEnabled(state);
        mSource.setEnabled(state);
        excludeOper.setEnabled(state);
        mDepartment.setEnabled(state);
        anchorExample.setEnabled(state);
    }

    private Button createShowButton() {
        return createShowPackageButton(false);
    }

    private Button createErrorButton() {
        return createShowPackageButton(true);
    }

    private Button createShowPackageButton(final boolean error){
        Button btn = new Button();
        btn.setText(error ? "Ошибки загрузки" : "Просмотр пакета");
        btn.addStyleName("dlg-button");

        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new BatchPostingFormDlg(error) {
                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            return true;
                        }

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[] {idPackage};
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return btn;
    }


    @Override
    protected boolean onClickOK() throws Exception {
        params = idPackage;
        return idPackage != null;
    }

    public String getCaption() {
        return caption;
    }
}
