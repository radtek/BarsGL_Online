package ru.rbt.barsgl.ejb.props;

/**
 * Created by ER21006 on 11.04.2016.
 */
public enum PropertyName {

    PD_CONCURENCY("pd.cuncurency")
    , ETLPKG_PROCESS_COUNT("etlpkg.process.count")
    , BATPKG_PROCESS_COUNT("batpkg.process.count")
    , MANUAL_PROCESS_COUNT("manual.process.count")
    , BATCH_PROCESS_ALLOWED("manual.process.allowed")
    , AD_LDAP_URI("auth.ldapURI")
    , BATPKG_MAXROWS("batpkg.max.count")
    , MOVEMENT_TIMEOUT("mvmt.timeout.sec");

    private String name;

    PropertyName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
