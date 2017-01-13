package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by SotnikovAV on 24.10.2016.
 */
public class ColumnBuilder {

    private Column column = new Column();

    private ColumnBuilder() {
    }

    public static final ColumnBuilder newInstance() {
        return new ColumnBuilder();
    }

    /**
     * Видимый заголовок
     * @param caption
     */
    public ColumnBuilder caption(String caption) {
        column.setCaption(caption);
        return this;
    }

    /**
     * Имя поля в таблице БД
     * @param name
     */
    public ColumnBuilder name(String name) {
        column.setName(name);
        return this;
    }

    /**
     * Тип колонки
     * @param type
     */
    public ColumnBuilder type(Column.Type type) {
        column.setType(type);
        return this;
    }

    /**
     * Тип сортировки колонки
     * @param sortType
     */
    public ColumnBuilder sortType(Column.Sort sortType) {
        column.setSortable(true);
        column.setSortType(sortType);
        return this;
    }

    /**
     * Формат поля (для полей, у которых есть формат и не устраивает формат по умолчанию)
     * @param format
     */
    public ColumnBuilder format(String format) {
        column.setFormat(format);
        return this;
    }

    /**
     * Ширина в пикселях
     * @param width
     */
    public ColumnBuilder width(int width) {
        column.setWidth(width);
        return this;
    }

    /**
     * Допустимы ли значения NULL для колонки
     * @param nullable
     */
    public ColumnBuilder nullable(boolean nullable) {
        column.setNullable(nullable);
        return this;
    }

    /**
     * Видимость колонки
     * @param visible
     */
    public ColumnBuilder visible(boolean visible) {
        column.setVisible(visible);
        return this;
    }

    /**
     * Показывать колонку на форме редактирования
     * @param editable
     */
    public ColumnBuilder editable(boolean editable) {
        column.setEditable(editable);
        return this;
    }

    /**
     * Колонка только для чтения
     * @param readonly
     */
    public ColumnBuilder readonly(boolean readonly) {
        column.setReadonly(readonly);
        return this;
    }

    /**
     * Позволяет сортироваться по колонке
     * @param sortable
     */
    public ColumnBuilder sortable(boolean sortable) {
        column.setSortable(sortable);
        return this;
    }

    /**
     * Позволяет фильтроваться по колонке
     * @param filterable
     * @return
     */
    public ColumnBuilder filterable(boolean filterable) {
        column.setFilterable(filterable);
        return this;
    }

    /**
     * Максимальная длина поля (пример varchar(10))
     * @param maxLength
     * @return
     */
    public ColumnBuilder maxLength(int maxLength) {
        column.setMaxLength(maxLength);
        return this;
    }

    /**
     * Значение по умолчанию
     * @param defValue
     * @return
     */
    public ColumnBuilder defaultValue(Field<Serializable> defValue) {
        column.setDefValue(defValue);
        return this;
    }

    /**
     * Многострочный текст
     * @param multiLine
     * @return
     */
    public ColumnBuilder multiLine(boolean multiLine) {
        column.setMultiLine(multiLine);
        return this;
    }

    /**
     *
     * @param list
     * @return
     */
    public ColumnBuilder list(HashMap<Serializable, String> list) {
        column.setList(list);
        return this;
    }

    /**
     * Добавить значение в список выбора
     * @param label
     * @param value
     * @return
     */
    public ColumnBuilder addListValue(String label, Serializable value) {
        HashMap<Serializable, String> list = column.getList();
        if(null == list) {
            list = new HashMap<>();
            column.setList(list);
        }
        list.put(value, label);
        return this;
    }

    /**
     * Создать колонку таблицы
     * @return
     * @throws Exception
     */
    public Column build() throws Exception {
        if (null == column.getFormat() || column.getFormat().isEmpty()) {
            switch (column.getType()) {
                case DATE:
                    column.setFormat("dd.MM.yyyy");
                    break;
                case DATETIME:
                    column.setFormat("dd.MM.yyyy HH:mm:ss");
                    break;
                case DECIMAL:
                    column.setFormat("#,##0.00");
                    break;
                case LONG:
                case INTEGER:
                    column.setFormat("##########");
                    break;
                default:
                    column.setFormat("");
                    break;
            }
        }
        return column;
    }

}
