package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.EnumSet;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.getWhereInClause;

/**
 * Created by akichigi on 20.06.16.
 */
public abstract class OperBase extends OperSuperBase {
    protected GridAction _stepChoiceAction;

    protected BatchPostStep _step = BatchPostStep.NOHAND;
    private Boolean _ownMessages;
    private StepChoiceDlg.MessageType _type = StepChoiceDlg.MessageType.ALL;
    private String _select = "";

    private String _where_base = "where (PST.PROCDATE = (select CURDATE from GL_OD)) and PST.INP_METHOD = '{0}' " +
                                 "and PST.INVISIBLE = '" + InvisibleType.N.name() + "' ";
    private String _where_auth = "";
    private String _where_step_part = "";
    private String _where_ownMessages = "";
    private String _where_message_type = "";

    public OperBase(String title) {
        super(title, true);
        _select = getSelectClause();
        _where_base = Utils.Fmt(_where_base, getInputMethod().name());
        _where_auth = initWhereAuthPart();
        setSql(sql());

        reconfigure();
        doActionVisibility();
        _stepChoiceAction.execute();
    }

    protected void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createPreview());
        abw.addAction(_stepChoiceAction = createStepChoice());
    }

    protected abstract InputMethod getInputMethod();
    protected abstract String getSelectClause();
    protected abstract void doActionVisibility();

    private GridAction createStepChoice(){
        return new GridAction(grid, null, "Выбор шага обработки", new Image(ImageConstants.INSTANCE.site_map()), 10) {
            StepChoiceDlg dlg;

            @Override
            public void execute() {
                dlg = new StepChoiceDlg(getInputMethod());
                dlg.setDlgEvents(this);
                Object[] prms = new Object[] {_step, _ownMessages, _type};
                dlg.show(prms);
            }

            public void onDlgOkClick(Object prms){
                dlg.hide();

                _step = (BatchPostStep)((Object[])prms)[0];
                _ownMessages = (Boolean)((Object[])prms)[1];
                _type = (StepChoiceDlg.MessageType)((Object[])prms)[2];

                FormManagerUI.ChangeStatusBarText("Шаг обработки: " + (_step.isNoneStep() ? "" : _step.getLabel()), FormManagerUI.MessageReason.MSG);
                doActionVisibility();

                changeWhereStepPart();
                changeWhereMessageTypePart();
                changeWhereOwnMessagesPart();

                setSql(sql());
                //System.out.println(sql());
                refreshAction.execute();
            }
        };
    }



    @Override
    protected String prepareSql() {
        return null;
    }

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }

    private String sql(){
        return new StringBuilder()
                .append(_select )
                .append(_where_base)
                .append(_where_auth)
                .append(_where_step_part)
                .append(_where_message_type)
                .append(_where_ownMessages).toString();
    }

    private void changeWhereStepPart(){
        _where_step_part = getOperStepClause(_step, "PST.STATE");
        if (! _where_step_part.isEmpty()) {
            _where_step_part = " and " + _where_step_part;
        }
    }

    //Where Clauses
    private String getOperStepClause(BatchPostStep step, String field){
        EnumSet<BatchPostStatus> statuses;

        switch (step) {
            case HAND1:
                statuses = EnumSet.of(BatchPostStatus.INPUT, BatchPostStatus.REFUSE);
                break;
            case HAND2:
                statuses = EnumSet.of(BatchPostStatus.CONTROL, BatchPostStatus.SIGNED, BatchPostStatus.SIGNEDVIEW, BatchPostStatus.REFUSEDATE,
                        BatchPostStatus.ERRPROC, BatchPostStatus.ERRSRV, BatchPostStatus.REFUSESRV);
                break;
            case HAND3:
                statuses = EnumSet.of(BatchPostStatus.WAITDATE, BatchPostStatus.SIGNEDDATE, BatchPostStatus.ERRPROCDATE
                        /*,BatchPostStatus.ERRSRVDATE, BatchPostStatus.REFUSESRVDATE*/);
                break;
            default:
                statuses = null;
                break;
        }
        return getWhereInClause(statuses, field);
    }

    private String getOwnMessagesClause(Boolean ownMessages, BatchPostStep step){
        if (!ownMessages) return "";

        String field;
        switch (step){
            case HAND1:
                field = "PST.USER_NAME";
                break;
            case HAND2:
                field = "PST.USER_AU2";
                break;
            case HAND3:
                field = "PST.USER_AU3";
                break;
            default:
                field = "";
                break;
        }

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper == null) return "";

        return  field.isEmpty() ? " and " + "'" + wrapper.getUserName() + "' in (PST.USER_NAME, PST.USER_AU2, PST.USER_AU3, PST.USER_CHNG)"
                : " and " + field + "=" + "'" + wrapper.getUserName() + "'";
    }

    private void changeWhereOwnMessagesPart(){
        _where_ownMessages = getOwnMessagesClause(_ownMessages, _step);
    }

    private void changeWhereMessageTypePart(){
        String predicate = "";

        switch (_type){
            case COMPLETED:
                predicate = " and PST.STATE='" + BatchPostStatus.COMPLETED.name() + "'";
                break;
            case NOTCOMPLETED:
                predicate = " and PST.STATE<>'" + BatchPostStatus.COMPLETED.name() + "'";
                break;
            default:
                predicate = "";
                break;
        }
        _where_message_type = predicate;
    }

    private String initWhereAuthPart(){
        return getSourceAndFilialPart("and", "PST.SRC_PST", "PST.CBCC_CR", "PST.CBCC_DR");
    }
}
