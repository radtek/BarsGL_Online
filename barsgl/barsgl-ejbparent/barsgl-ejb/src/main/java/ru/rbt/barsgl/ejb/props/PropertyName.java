package ru.rbt.barsgl.ejb.props;

/**
 * Created by ER21006 on 11.04.2016.
 */
public enum PropertyName {

    PD_CONCURENCY("pd.cuncurency")
    , ETLPKG_PROCESS_COUNT("etlpkg.process.count");

    private String name;

    PropertyName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
