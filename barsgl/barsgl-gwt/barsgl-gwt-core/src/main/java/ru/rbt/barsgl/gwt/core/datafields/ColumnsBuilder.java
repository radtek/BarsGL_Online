package ru.rbt.barsgl.gwt.core.datafields;

/**
 * Created by Ivan Sevastyanov
 */
public class ColumnsBuilder {

    private final Columns cols = new Columns();

    public ColumnsBuilder addColumn(String name, Column.Type type) {
        return addColumn(new Column(name, type));
    }

    public ColumnsBuilder addColumn(Column column) {
        if (0 > cols.addColumn(column)) {
            throw new RuntimeException("Illegal index on building columns");
        }
        return this;
    }

    public Columns build() {
        return cols;
    }
}
