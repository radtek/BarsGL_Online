package ru.rbt.barsgl.ejb.props;

/**
 * Created by Ivan Sevastyanov on 13.05.2016.
 */
public enum NodeName {

    NodeDnameRules("auth.dname.regexps");

    private String name;

    NodeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
