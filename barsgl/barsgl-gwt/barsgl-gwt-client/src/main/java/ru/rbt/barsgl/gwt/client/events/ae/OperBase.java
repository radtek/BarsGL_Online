package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import java.util.EnumSet;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.getWhereInClause;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by akichigi on 20.06.16.
 */
public abstract class OperBase extends OperSuperBase {
    protected GridAction _stepChoiceAction;
    protected GridAction _packageStatAction;

    protected BatchPostStep _step = BatchPostStep.NOHAND;
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
        abw.addAction(_packageStatAction = new PackageStatisticsAction(grid));
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

            @Override
            public void onDlgOkClick(Object prms){
                dlg.hide();

                _step = (BatchPostStep)((Object[])prms)[0];
                _ownMessages = (Boolean)((Object[])prms)[1];
                _type = (StepChoiceDlg.MessageType)((Object[])prms)[2];

                StatusBarManager.ChangeStatusBarText("Шаг обработки: " + (_step.isNoneStep() ? "" : _step.getLabel()), StatusBarManager.MessageReason.MSG);
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
                statuses = EnumSet.of(INPUT, REFUSE);
                break;
            case HAND2:
                statuses = EnumSet.of(CONTROL, REFUSEDATE, ERRPROC, ERRSRV, REFUSESRV, TIMEOUTSRV);
                break;
            case HAND3:
                statuses = EnumSet.of(WAITDATE, ERRPROCDATE);
                break;
            default:
                statuses = null;
                break;
        }
        return getWhereInClause(statuses, field);
    }

    private void changeWhereOwnMessagesPart(){
        _where_ownMessages = getOwnMessagesClause(_ownMessages, _step, "PST");
    }

    private void changeWhereMessageTypePart(){
        _where_message_type = getWhereMessageTypePart("PST");
    }

    private String initWhereAuthPart(){
        return getSourceAndFilialPart("and", "PST.SRC_PST", "PST.CBCC_CR", "PST.CBCC_DR");
    }
}
