package ru.rbt.barsgl.gwt.client.load;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.dictionary.BatchPostingFormDlg;
import ru.rbt.barsgl.gwt.client.check.CheckFileExtention;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by Ivan Sevastyanov
 */
@Deprecated
public class LoadForm extends BaseForm {
    public static final String FORM_NAME = "Загрузка Excel файлов";
    private final static String LIST_DELIMITER = "#";

    private final String LABEL_WIDTH = "110px";
    private final String FIELD_WIDTH = "250px";

    private FormPanel formPanel;
    private DataListBox mSource;
    private DataListBox mDepartment;
    private FileUpload fileUpload;

    private Button errorButton;
    private Panel loadingResult;

    private Hidden fileName;
    private Hidden source;
    private Hidden department ;

    private Long idPackage;

    public LoadForm() {
        super();
        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {
        return createUploadPanel();
    }

    private Panel createUploadPanel() {

        VerticalPanel panel = new VerticalPanel();

        Grid g = new Grid(2, 2);
        panel.add(g);
        mSource = createDealSourceListBox("", FIELD_WIDTH);
        g.setWidget(0, 0, createLabel("Источник сделки", LABEL_WIDTH));
        g.setWidget(0, 1, mSource);

        mDepartment = createDepartmentListBox("", FIELD_WIDTH, true);
        g.setWidget(1, 0, createLabel("Подразделение", LABEL_WIDTH));
        g.setWidget(1, 1, mDepartment);

        Label selectLabel = createLabel("Файл для загрузки");
        panel.add(selectLabel);

        fileUpload = new FileUpload();
        fileUpload.setName("excel-upload001");
        fileUpload.setWidth("400px");
        panel.add(fileUpload);

        fileName = new Hidden();
        fileName.setName("filename");
        panel.add(fileName);

        source = new Hidden();
        source.setName("source");
        panel.add(source);

        department = new Hidden();
        department.setName("department");
        panel.add(department);

        Grid g2 = new Grid(1, 2);
        panel.add(g2);
        g2.setWidget(0, 0, createUploadButton());
        g2.setWidget(0, 1, errorButton = createErrorButton());
        errorButton.setEnabled(false);
        panel.add(g2);

        loadingResult = new VerticalPanel();
        panel.add(loadingResult);

        formPanel = new FormPanel();
        formPanel.setWidget(panel);
        formPanel.setAction("service/UploadFileHandler");
        // set form to use the POST method, and multipart MIME encoding.
        formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        formPanel.setMethod(FormPanel.METHOD_POST);

        final String urlExample = GWT.getHostPageBaseURL() + "excel/example.xlsx";
        HorizontalPanel hp = new HorizontalPanel();
        hp.add(new Label("Скачать пример "));
        Anchor anchorExample = new Anchor("Example.xlsx");
        anchorExample.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
        anchorExample.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.open(urlExample,
                        "_self", "disabled");
            }
        });
        hp.add(anchorExample);


        panel.add(hp);

        formPanel.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                loadingResult.clear();
                String message = parceResponce(event.getResults());
                if (!isEmpty(message)) {
                    HTML safeHtml = new HTML(message.replaceAll(LIST_DELIMITER, "<BR>"));
                    loadingResult.add(safeHtml);
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
        // Add form to the root panel.

        return formPanel;
    }

    private String parceResponce(String responce) {
        try {
            errorButton.setEnabled(false);
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
        if (!isEmpty(stringWithNumber)
                && (index = stringWithNumber.indexOf(delim)) >= 0) {
            try {
                return Long.decode(stringWithNumber.substring(index+1).trim());
            } catch (Exception e) {
                loadingResult.add(new HTML(e.getMessage()));
            }
        }
        return null;
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
                    errorButton.setEnabled(false);
                    loadingResult.clear();
                    CheckNotEmptyString check = new CheckNotEmptyString();
                    source.setValue(check((String) mSource.getValue()
                            , "Источник сделки", "поле не заполнено", check));
                    department.setValue(check((String) mDepartment.getValue()
                            , "Подразделение", "поле не заполнено", check));
                    String filename = check(fileUpload.getFilename(), "Файл для загрузки", "не выбран", check);
                    fileName.setValue(check(filename, "Файл для загрузки", "нужен файл типа 'xlsx'", new CheckFileExtention("xlsx")));

                    loadingResult.add(new Label("Ожидайте, идет загрузка проводок из файла ..."));
                    formPanel.submit();

                } catch (IllegalArgumentException e) {
                    if (e.getMessage() == null || !e.getMessage().equals("column")) {
                        throw e;
                    }
                }
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
                try {
                    GridFormDlgBase dlg = new BatchPostingFormDlg(true) {

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
}
