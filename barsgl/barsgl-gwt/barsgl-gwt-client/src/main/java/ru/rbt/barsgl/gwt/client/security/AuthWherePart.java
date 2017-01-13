package ru.rbt.barsgl.gwt.client.security;

import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.List;

/**
 * Created by akichigi on 23.06.16.
 */
public class AuthWherePart {
    private static String emptyWhere(){
        //TODO ??? Если не грантовано, нужно показывать все или совсем ничего!
        return "";
    }

    public static String getSourcePart(String startSymbol, String sourceField){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        String whereSource = "";

        if (wrapper != null){
            whereSource = WhereClauseBuilder.getWhereOperatorClause(sourceField, wrapper.getGrantedSources(), "or");
        }

        return  whereSource.isEmpty()
                ? emptyWhere()
                : Utils.Fmt(" {0} {1}", startSymbol, whereSource);
    }

    public static String getFilialPart(String startSymbol, String filialField){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        String whereFilial = "";

        if (wrapper != null){
            whereFilial = WhereClauseBuilder.getWhereOperatorClause(filialField, wrapper.getGrantedHeadBranches(), "or");
        }

        return  whereFilial.isEmpty()
                ? emptyWhere()
                : Utils.Fmt(" {0} {1}", startSymbol, whereFilial);
    }

    public static String getSourceAndFilialPart(String startSymbol, String sourceField, String filialFieldA, String filialFieldB){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        String whereFilialA = "";
        String whereFilialB = "";
        String whereSource = "";
        if (wrapper != null){
            whereFilialB = WhereClauseBuilder.getWhereOperatorClause(filialFieldB, wrapper.getGrantedHeadBranches(), "or");
            whereFilialA = WhereClauseBuilder.getWhereOperatorClause(filialFieldA, wrapper.getGrantedHeadBranches(), "or");
            whereSource = WhereClauseBuilder.getWhereOperatorClause(sourceField, wrapper.getGrantedSources(), "or");
        }
        // if whereFilialCredit is empty then whereFilialDebet is empty too!
        String where = (whereFilialA.isEmpty() && whereSource.isEmpty())
                ? emptyWhere()
                : Utils.Fmt(" {0} {1} {2} {3}",
                startSymbol,
                whereFilialA.isEmpty() ?  "" : "(" + whereFilialB + " or " + whereFilialA + ")",
                whereSource.isEmpty() || whereFilialA.isEmpty() ? "" : "and",
                whereSource);
        return where;
    }

    public static String getSourceAndFilialPart(String startSymbol, String sourceField, String filialField){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        String whereFilial = "";
        String whereSource = "";
        if (wrapper != null){
            whereFilial = WhereClauseBuilder.getWhereOperatorClause(filialField, wrapper.getGrantedHeadBranches(), "or");
            whereSource = WhereClauseBuilder.getWhereOperatorClause(sourceField, wrapper.getGrantedSources(), "or");
        }

        String where = (whereFilial.isEmpty() && whereSource.isEmpty())
                ? emptyWhere()
                : Utils.Fmt(" {0} {1} {2} {3}",
                startSymbol,
                whereFilial,
                whereFilial.isEmpty() || whereSource.isEmpty() ? "" : "and",
                whereSource);
        return where;
    }

    public static String getSourceAndCodeFilialPart(String startSymbol, String sourceField, String filialFieldA, String filialFieldB){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        String whereFilial = "";
        String whereSource = "";
        if (wrapper != null){
            whereFilial =  getWhereFilialClause(filialFieldA, filialFieldB, wrapper.getGrantedHeadBranches());
            whereSource = WhereClauseBuilder.getWhereOperatorClause(sourceField, wrapper.getGrantedSources(), "or");
        }

        String where = (whereFilial.isEmpty() && whereSource.isEmpty())
                ? emptyWhere()
                : Utils.Fmt(" {0} {1} {2} {3}",
                startSymbol,
                whereFilial,
                whereFilial.isEmpty() || whereSource.isEmpty() ? "" : "and",
                whereSource);
        return where;
    }

    private static String getWhereFilialClause(String fieldA, String fieldB, List<String> values){
        if (values.isEmpty()) return "";
        if (values.contains("*")) return "";
        return  fieldB.isEmpty()
                ? Utils.Fmt("('{0}'={1})", values.get(0), fieldA)
                : Utils.Fmt("('{0}' in ({1}, {2}))", values.get(0), fieldA, fieldB);
    }
}
