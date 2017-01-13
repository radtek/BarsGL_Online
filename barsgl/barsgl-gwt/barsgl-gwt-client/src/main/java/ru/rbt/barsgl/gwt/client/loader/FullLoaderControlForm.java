package ru.rbt.barsgl.gwt.client.loader;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by SotnikovAV on 03.11.2016.
 */
public class FullLoaderControlForm extends BaseForm {

    /**
     * Формы
     */
    private Map<Integer, BaseForm> forms = new HashMap<>();
    /**
     * Главная панель формы
     */
    private DockLayoutPanel mainPanel;
    /**
     * Панель для содержимого закладок
     */
    private DockLayoutPanel formPanel;
    /**
     * Закладки
     */
    private TabBar header;
    /**
     * Текущая форма
     */
    private BaseForm currentForm;
    /**
     * Индекс текущей формы
     */
    private Integer currentFormNumber = 0;

    /**
     * Получить массив коротких наименований закладок
     *
     * @return массив коротких наименований закладок
     */
    protected String[] getShortTabNames() {
        return new String []{"BARSGL", "BARSREP"};
    }

    /**
     * Получить массив длинных наименований закладок
     *
     * @return массив длинных наименований закладок
     */
    protected String[] getLongTabNames() {
        return new String [] {"BARSGL", "BARSREP"};
    }

    /**
     * Получить форму по индексу. Метод должен содержать что-то типа этого:
     *
     * <pre>
     * {@code switch (index) {
     *	case 0:
     *		return new TechN1_Page1();
     *	case 1:
     *		return new TechN1_Page2();
     *	case 2:
     *		return new TechN1_Page3();
     *	case 3:
     *		return new TechN1_Page4();
     *	default:
     *		throw new Exception("Форма с индексом " + index + " не найдена!");
     *	}}
     * </pre>
     *
     * @param index
     *            - индекс формы
     * @return экземпляр формы
     * @throws Exception
     */
    protected BaseForm getFormByIndex(int index) throws Exception {
        switch(index) {
            case 0:
                return new BarsglLoaderControlForm();
            case 1:
                return new BarsrepLoaderControlForm();
            default:
                throw new Exception("Форма с индексом " + index + " не найдена!");
        }
    }

    /**
     * Перейти на закладку с указанным индексом
     *
     * @param page
     *            - индекс
     */
    protected void changePage(Integer page) {

        if (forms.containsKey(page)) {
            currentForm = forms.get(page);
        } else {
            try {
                currentForm = getFormByIndex(page);
                forms.put(page, currentForm);
            } catch (Exception e) {
                Window.alert("Ошибка при открытии закладки: " + e.toString());
            }

        }
        formPanel.clear();
        formPanel.add(currentForm);

        header.setTabHTML(currentFormNumber, "<a title='" + getLongTabNames()[currentFormNumber] + "'>"
                + getShortTabNames()[currentFormNumber] + "</a>");
        header.setTabText(page, getLongTabNames()[page]);

        currentFormNumber = page;

        header.selectTab(currentFormNumber, false);

    }

    /**
     * @return the forms
     */
    public Map<Integer, BaseForm> getForms() {
        return forms;
    }

    /**
     * @param forms
     *            the forms to set
     */
    public void setForms(Map<Integer, BaseForm> forms) {
        this.forms = forms;
    }

    /**
     * @return the formPanel
     */
    public DockLayoutPanel getFormPanel() {
        return formPanel;
    }

    /**
     * @param formPanel
     *            the formPanel to set
     */
    public void setFormPanel(DockLayoutPanel formPanel) {
        this.formPanel = formPanel;
    }

    /**
     * @return the header
     */
    public TabBar getHeader() {
        return header;
    }

    /**
     * @param header
     *            the header to set
     */
    public void setHeader(TabBar header) {
        this.header = header;
    }

    /**
     * @return the currentForm
     */
    public BaseForm getCurrentForm() {
        return currentForm;
    }

    /**
     * @param currentForm
     *            the currentForm to set
     */
    public void setCurrentForm(BaseForm currentForm) {
        this.currentForm = currentForm;
    }

    /**
     * @return the currentFormNumber
     */
    public Integer getCurrentFormNumber() {
        return currentFormNumber;
    }

    /**
     * @param currentFormNumber
     *            the currentFormNumber to set
     */
    public void setCurrentFormNumber(Integer currentFormNumber) {
        this.currentFormNumber = currentFormNumber;
    }

    @Override
    public Widget createContent() {
        mainPanel = new DockLayoutPanel(Style.Unit.MM);
        formPanel = new DockLayoutPanel(Style.Unit.MM);

        header = new TabBar();
        for (int i = 0; i < getShortTabNames().length; i++) {
            header.addTab(
                    new HTML("<a title='" + getLongTabNames()[i] + "'>" + getShortTabNames()[i] + "</a>").asWidget());
        }
        header.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                changePage(event.getSelectedItem());
            }
        });

        mainPanel.addNorth(header, 10);
        mainPanel.add(formPanel);

        return mainPanel;
    }

}
