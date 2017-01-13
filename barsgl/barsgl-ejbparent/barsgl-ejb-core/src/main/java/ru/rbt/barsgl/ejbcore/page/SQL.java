package ru.rbt.barsgl.ejbcore.page;

/**
 * Created by Ivan Sevastyanov
 */
public class SQL {

    private final String query;
    private final Object[] params;

    public SQL(String query, Object... params) {
        this.query = query;
        this.params = params == null ? new Object[0] : params;
    }

    public String getQuery() {
        return query;
    }

    public Object[] getParams() {
        return params;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (!SQL.class.equals(obj.getClass()))) {
            return false;
        }
        SQL sf = (SQL)obj;
        if (!query.equals(sf.query) || (params.length != sf.params.length)) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                if (sf.params[i] != null) {
                    return false;
                } else {
                    continue;
                }
            }
            if (!params[i].equals(sf.params[i])) {
                return false;
            }
        }
        return true;
    }

    public void printDebugInfo() {
        System.out.println(query);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {

                System.out.println("  [" + i + "] " +
                                ((params[i] == null) ? "" : params[i].getClass().getName() + " \t= ") +
                                params[i]
                );
            }
        }
    }
}
