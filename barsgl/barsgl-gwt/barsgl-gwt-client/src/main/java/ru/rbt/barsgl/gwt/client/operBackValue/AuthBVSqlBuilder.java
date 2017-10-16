package ru.rbt.barsgl.gwt.client.operBackValue;

import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.shared.user.AppUserWrapper;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;

/**
 * Created by er17503 on 09.08.2017.
 */
public class AuthBVSqlBuilder {
    private String _STANDARD_MANUAL_SELECT = "select * from (" +
            "select op.*, ex.POSTDATE_PLAN, case when op.POSTDATE = ex.POSTDATE_PLAN then 'N' else 'Y' end as PDATE_CHNG, " +
            "ex.MNL_RSNCODE, ex.BV_CUTDATE, ex.PRD_LDATE, ex.PRD_CUTDATE, ex.MNL_NRT, " +
            "ex.MNL_STATUS, ex.USER_AU3, ex.OTS_AU3, ex.OTS_AUTO, " +
            "u.SURNAME || ' ' ||  LEFT(u.FIRSTNAME, 1) || '.' || LEFT(COALESCE(u.PATRONYMIC, ''), 1) || " +
            "case when COALESCE(u.PATRONYMIC, '') = '' then '' else '.' end as AUTHOR " +
            "from V_GL_OPERCUST as op " +
            "join GL_OPEREXT ex on op.GLOID = ex.GLOID " +
            "left join GL_USER u on ex.USER_AU3 = u.USER_NAME) v ";

    private String _STANDARD_MANUAL_WHERE = "where VDATE < PROCDATE and MNL_STATUS = 'COMPLETED' ";

    private String _BASE_SELECT = "select op.*, cast(null as DATE) as POSTDATE_PLAN, '' as PDATE_CHNG, '' as MNL_RSNCODE, " +
                                  "cast(null as DATE) as BV_CUTDATE, cast(null as DATE) as PRD_LDATE, cast(null as DATE) as PRD_CUTDATE, " +
                                  "'' as MNL_NRT, '' as MNL_STATUS, '' as USER_AU3, cast(null as TIMESTAMP) as OTS_AU3, " +
                                  "cast(null as TIMESTAMP) as OTS_AUTO, '' as AUTHOR " +
                                  "from V_GL_OPERCUST op ";
    private String _BASE_WHERE = "where op.VDATE < op.PROCDATE and op.STATE ='POST' and op.OPER_CLASS <> 'BV_MANUAL' and op.INP_METHOD = 'AE' ";
    private String _STANDARD_AUTO_WHERE = "and op.FAN <> 'Y' and op.SRC_PST <>'K+TP' ";
    private String _NONSTANDARD_KPlusTP_WHERE = "and op.FAN <> 'Y' and op.SRC_PST ='K+TP' ";
    private String _NONSTANDARD_FAN_WHERE =  "and op.FAN = 'Y' ";

    private String _CONSOLIDATE_FAN_SELECT = "select v.*, cast(null as DATE) as POSTDATE_PLAN, '' as PDATE_CHNG, '' as MNL_RSNCODE, cast(null as DATE) as BV_CUTDATE, " +
                                             "cast(null as DATE) as PRD_LDATE, cast(null as DATE) as PRD_CUTDATE, '' as MNL_NRT, " +
                                             "'' as MNL_STATUS, '' as USER_AU3, cast(null as TIMESTAMP) as OTS_AU3, " +
                                             "cast(null as TIMESTAMP) as OTS_AUTO, '' as AUTHOR " +
                                             "from V_GL_OPERFAN_BV v " +
                                              getSourceAndFilialPart("where", "SRC_PST", "CBCC_CR", "CBCC_DR");

    private String user;

    public AuthBVSqlBuilder(){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        user = wrapper == null ? "" : wrapper.getUserName();
    }

    private boolean consolidateFAN = false;

    public String buildSql(BVOperChoiceDlg.ModeType modeType, BVOperChoiceDlg.SpecType spec, boolean owner){
        String _sql = "";

        switch (modeType){
            case STANDARD:
                if (spec == BVOperChoiceDlg.SpecType.MANUAL){
                    _sql = _STANDARD_MANUAL_SELECT + _STANDARD_MANUAL_WHERE + (owner ? Utils.Fmt("and USER_AU3='{0}'", user) : "");
                }
                else if (spec == BVOperChoiceDlg.SpecType.AUTO){
                    _sql = _BASE_SELECT + _BASE_WHERE + _STANDARD_AUTO_WHERE;
                }
                else return "";
                break;
            case NONSTANDARD:
                if (spec == BVOperChoiceDlg.SpecType.K_Plus_TP){
                    _sql = _BASE_SELECT + _BASE_WHERE + _NONSTANDARD_KPlusTP_WHERE;
                }
                else if (spec == BVOperChoiceDlg.SpecType.FAN){
                    _sql = consolidateFAN ? _CONSOLIDATE_FAN_SELECT : _BASE_SELECT + _BASE_WHERE + _NONSTANDARD_FAN_WHERE;
                }
                else return "";
                break;
        }

        return _sql + getAuthWherePart();
    }

    private String getAuthWherePart(){
        return getSourceAndFilialPart("and", "SRC_PST", "CBCC_CR", "CBCC_DR");
    }

    public void setConsolidateFAN(boolean consolidateFAN) {
        this.consolidateFAN = consolidateFAN;
    }
}
