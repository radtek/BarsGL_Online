package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.compLookup.LookUpAcc2;
import ru.rbt.barsgl.gwt.client.compLookup.LookUpPlcode;
import ru.rbt.barsgl.gwt.client.compLookup.LookupBoxBase;
import ru.rbt.barsgl.gwt.client.dict.*;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.BtnTxtBox;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.ActParmWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 24.08.16.
 */
public class ActParmDlg extends DlgFrame implements IAfterShowEvent {
    public final static String REQUIRED = " обязательно для заполнения";
    public final static String EDIT = "Редактирование параметров счета";
    public final static String CREATE = "Ввод параметров счета";
    public final static String DELETE = "Удаление параметров счета";

    private final static String OK_CAPTION = "Выбрать";

    private BtnTxtBox accType;
    private boolean _isTech;
    private BtnTxtBox cusType;
    private BtnTxtBox term;
    private LookupBoxBase acc2;
    private BtnTxtBox plcode;
    private BtnTxtBox acod;
    private TxtBox ac_sq;
    private DatePickerBox dtb;
    private DatePickerBox dte;
    private PushButton generator;

    private FormAction action;
    private ActParmWrapper wrapper;

    public void setFormAction(FormAction action){
        this.action = action;
        String caption = "";
        switch (action) {
            case CREATE:
                ok.setText("Создать");
                caption = CREATE;
                break;
            case UPDATE:
                ok.setText(TEXT_CONSTANTS.formData_Update());
                caption = EDIT;
                break;
            case DELETE:
                ok.setText(TEXT_CONSTANTS.formData_Delete());
                caption = DELETE;
                break;
        }
        setCaption(caption);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(10, 2);
        grid.setWidget(0, 0, new Label(ActParm.FIELD_ACCTYPE));
        grid.setWidget(0, 1, accType = new BtnTxtBox() {
            @Override
            public void onBntClick() {
                lookUpAccType();
            }
        });
        accType.setMaxLength(9);
        accType.setWidth("95px");
        accType.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(ActParm.FIELD_CUSTYPE));
        grid.setWidget(1, 1, cusType = new BtnTxtBox() {
            @Override
            public void onBntClick() {
                lookUpProperty();
            }
        });
        cusType.setMaxLength(3);
        cusType.setWidth("95px");
        cusType.setMask("[0-9]");

        grid.setWidget(2, 0, new Label(ActParm.FIELD_TERM));
        grid.setWidget(2, 1, term = new BtnTxtBox() {
            @Override
            public void onBntClick() {
                lookUpTerm();
            }
        });
        term.setMaxLength(2);
        term.setWidth("95px");
        term.setMask("[0-9]");

        grid.setWidget(3, 0, new Label(ActParm.FIELD_ACC2));
        grid.setWidget(3, 1, acc2 = new LookUpAcc2("95px"){
            @Override
            protected void onSetResult() {
                initDteField();
                plcode.setFocus(true);
            }
        });
        acc2.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                initDteField();
            }
        });

        grid.setWidget(4, 0, new Label(ActParm.FIELD_PLCODE));
        grid.setWidget(4, 1, plcode = new LookUpPlcode("95px") {
            @Override
            protected void onSetResult() {
                acod.setFocus(true);
            }
        });

        grid.setWidget(5, 0, new Label(ActParm.FIELD_ACOD));
        HorizontalPanel hp = new HorizontalPanel();
        acod = new BtnTxtBox() {
            @Override
            public void onBntClick() {
                lookUpAcod();
            }
        };

        acod.setMaxLength(4);
        acod.setWidth("95px");
        acod.setMask("[0-9]");

        generator = new PushButton(new Image(ImageConstants.INSTANCE.dice()), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
               getFreeAcod();
            }
        });
        generator.setHeight("16px");
        generator.setWidth("16px");
        generator.setTitle("Следующий свободный ACOD");
       // generator.getElement().getStyle().setLineHeight(16, Style.Unit.PX);
        generator.getElement().getStyle().setMarginTop(1, Style.Unit.PX);

        hp.add(acod);
        hp.add(generator);
        grid.setWidget(5, 1, hp);

        grid.setWidget(6, 0, new Label(ActParm.FIELD_AC_SQ));
        grid.setWidget(6, 1, ac_sq = new TxtBox());
        ac_sq.setMaxLength(2);
        ac_sq.setWidth("95px");
        ac_sq.setMask("[0-9]");

        grid.setWidget(7, 0, new Label(ActParm.FIELD_DTB));
        grid.setWidget(7, 1, dtb = new DatePickerBox());
        dtb.setWidth("95px");

        grid.setWidget(8, 0, new Label(ActParm.FIELD_DTE));
        grid.setWidget(8, 1, dte = new DatePickerBox());
        dte.setWidth("95px");

        setAfterShowEvent(this);

        return grid;
    }

    private void initDteField(){
        Date d8 = new Date(108, 0, 1);
        Date d16 = new Date(116, 0, 1);
        if (dtb.getValue() != null && (dtb.getValue().compareTo(d8)!= 0 && dtb.getValue().compareTo(d16)!= 0))
            return;

        if (acc2.getValue() != null && acc2.getValue().startsWith("706")){
            dtb.setValue(d16);
        } else {
            dtb.setValue(d8);
        }
    }

    private void lookUpAccType(){
        GridFormDlgBase dlg = new GridFormDlgBase("Справочник плана счетов по AccType") {
            @Override
            protected GridForm getGridForm() {
               //for cache
              /*  AccTypeLookUp form = (AccTypeLookUp) LocalDataStorage.getParam(AccTypeLookUp.class.getSimpleName());
                if (form == null) {
                    form = new AccTypeLookUp();
                    LocalDataStorage.putParam(AccTypeLookUp.class.getSimpleName(), form);
                }
                 return form;*/
                 return new AccTypeLookUp();
            }

           @Override
           protected String getGridWidth() {
              return "700px";
          }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    accType.setValue(Utils.value((String) result.get("ACCTYPE")));
                    _isTech  = result.get("TECH_ACT").equals("Y");
                    cusType.setFocus(true);
                    setTechFields(_isTech);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void setTechFields(boolean isTech) {
        Date defDate = new Date(117,1,22);
        if (!cusType.hasValue())
            if (isTech) cusType.setValue("00"); else cusType.clear();

        if (!term.hasValue())
            if (isTech) term.setValue("00"); else term.clear();

        if (!acc2.hasValue())
            if ((isTech) && (accType.hasValue())) acc2.setValue("00"+accType.getValue().substring(0,3)); else acc2.clear();

        if (!dtb.hasValue())
            if (isTech) dtb.setValue(defDate); else dtb.clear();

        cusType.setReadOnly(isTech);
        term.setReadOnly(isTech);
        plcode.setReadOnly(isTech);
        acc2.setReadOnly(isTech);
    }

    private void lookUpTerm(){
        GridFormDlgBase dlg = new GridFormDlgBase("Справочник кодов срока") {
            @Override
            protected GridForm getGridForm() {
                return new TypesOfTerms(true);
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    term.setValue(Utils.value((String) result.get("TERM")));
                    acc2.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void lookUpProperty(){
        GridFormDlgBase dlg = new GridFormDlgBase("Справочник типов собственности") {
            @Override
            protected GridForm getGridForm() {
                return new PropertyType();
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    cusType.setValue(result.get("CTYPE").toString());
                    term.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void lookUpAcod(){
        GridFormDlgBase dlg = new GridFormDlgBase("Справочник кодов ACOD Midas") {
            @Override
            protected GridForm getGridForm() {
                return new AcodForm();
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    acod.setValue(Utils.value((String) result.get("ACOD")));
                    ac_sq.setFocus(true);
                }
                return true;
            }

            @Override
            protected String getGridWidth() {
                return "800px";
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void getFreeAcod(){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        try {
            BarsGLEntryPoint.dictionaryService.getFreeAcod(new AuthCheckAsyncCallback<RpcRes_Base<String>>() {
                @Override
                public void onSuccess(RpcRes_Base<String> res) {
                    if (res.isError()) {

                        DialogManager.error("Ошибка", res.getMessage());
                    } else {
                        acod.setValue(res.getResult());
                        ac_sq.setFocus(true);
                    }
                    WaitingManager.hide();
                }
            });
        } catch (Exception e) {
            DialogManager.error("Ошибка", e.getMessage());
        }
    }

    private void clear(){
        accType.clear();
        accType.setReadOnly(false);
        cusType.setReadOnly(false);
        cusType.clear();
        term.clear();
        term.setReadOnly(false);
        acc2.clear();
        acc2.setReadOnly(false);
        plcode.clear();
        plcode.setReadOnly(false);
        acod.clear();
        acod.setReadOnly(false);
        ac_sq.clear();
        ac_sq.setReadOnly(false);
        dtb.clear();
        dtb.setReadOnly(false);
        dte.clear();
        dte.setReadOnly(false);
    }

    @Override
    protected void fillContent() {
        clear();

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            Row row = (Row) params;

            accType.setValue(Utils.value((String) row.getField(0).getValue()));
            cusType.setValue(Utils.value((String) row.getField(1).getValue()));
            term.setValue(Utils.value((String) row.getField(2).getValue()));
            acc2.setValue(Utils.value((String) row.getField(3).getValue()));
            plcode.setValue(Utils.value((String) row.getField(4).getValue()));
            acod.setValue(Utils.value((String) row.getField(5).getValue()));
            ac_sq.setValue(Utils.value((String) row.getField(6).getValue()));
            dtb.setValue((Date) row.getField(7).getValue());
            dte.setValue((Date) row.getField(8).getValue());

            accType.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            cusType.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            term.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            acc2.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            plcode.setReadOnly(action == FormAction.DELETE);
            acod.setReadOnly(action == FormAction.DELETE);
            ac_sq.setReadOnly(action == FormAction.DELETE);
            dtb.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            dte.setReadOnly(action == FormAction.DELETE);
            generator.setVisible(action != FormAction.DELETE);

            _isTech = Utils.value((String)row.getField(10).getValue()).equalsIgnoreCase("Y")?true:Utils.value((String)row.getField(10).getValue()).equalsIgnoreCase("Да");

        }else{
             if (params != null){
                 Row row = (Row) params;
                 accType.setValue(row.getField(4).getValue().toString());
                 accType.setReadOnly(true);
                 _isTech = Utils.value((String)row.getField(8).getValue()).equalsIgnoreCase("Y");
                 //accType.setValue((String) params);
                 //accType.setReadOnly(true);
             }

            if (!_isTech) dtb.setValue(new Date(108, 0, 1));
        }
        setTechFields(_isTech);
    }

    private String checkRequeredString(String value, String columnCaption) {
        return check(value, columnCaption, REQUIRED, new AppPredicate<String>() {
            @Override
            public boolean check(String target) {
                return (target != null) ? !target.trim().isEmpty() : false;
            }
        });
    }

    private String checkLength(String value, int len, String fieldName){
        try{
            if (value != null && !value.trim().isEmpty() && value.trim().length() != len) throw new Exception( Utils.Fmt("Требуемое количество символов должно быть {0}", len));
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", fieldName, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        return value;
    }

    private void checkAcc70(String acc2, String plcode){
        try{
            if (acc2.startsWith("707")){
                throw new Exception("Недопустимое использование балансового счета 707xx. Используйте 706xx счет.");
            }
            if (acc2.startsWith("706") && ((plcode == null) || (plcode.trim().isEmpty()))){
                throw new Exception(Utils.Fmt("Для счетов доходов/расходов обязательно заполнение поля {0}", ActParm.FIELD_PLCODE));
            } else
                if (!acc2.startsWith("706") && ((plcode != null) && (!plcode.trim().isEmpty()))){
                    throw new Exception(Utils.Fmt("Поле {0} обязательно к заполнениею только для счетов доходов/расходов", ActParm.FIELD_PLCODE));
                }
        }catch(Exception e) {
            showInfo("Ошибка", e.getMessage());
            throw new IllegalArgumentException("column");
        }
    }

    private void checkAcc706(String acc2, String plcode){
        try {
             if (acc2.equals("70601") && !(plcode.startsWith("1") || (plcode.startsWith("2")))){
                 throw new Exception(Utils.Fmt("Для счета 70601 обязательно заполнение поля {0} значениями начинающимися с 1 или 2", ActParm.FIELD_PLCODE));
             }
            if (acc2.equals("70606") && !(plcode.startsWith("3") || (plcode.startsWith("4")))){
                throw new Exception(Utils.Fmt("Для счета 70606 обязательно заполнение поля {0} значениями начинающимися с 3 или 4", ActParm.FIELD_PLCODE));
            }
        }catch(Exception e) {
            showInfo("Ошибка", e.getMessage());
            throw new IllegalArgumentException("column");
        }
    }

    private String checkPlCode(String plCode, String acc2){
        try{
            if (acc2.startsWith("706")){
              if (plCode == null || plCode.trim().isEmpty() || plCode.trim().length() != 5){
                  throw new Exception(Utils.Fmt("Для счетов 706xx обязательно заполнение поля {0} длиной 5 символов", ActParm.FIELD_PLCODE));
              }
            }else{
              if ((plCode != null) && (!plCode.trim().isEmpty())){
                  throw  new Exception(Utils.Fmt("Поле {0} обязательно к заполнению только для счетов 706xx", ActParm.FIELD_PLCODE));
              }
            }
        }catch (Exception e){
            showInfo("Ошибка", e.getMessage());
            throw new IllegalArgumentException("column");
        }

        return  plCode;
    }

    private String checkAcod_sq(String sq){
        try{
            if (acod.getValue() != null && (!acod.getValue().isEmpty()) &&
               (sq == null || (sq.trim().isEmpty())))
                throw new Exception(Utils.Fmt("Поле {0} обязательно к заполнению при заполненном поле {1}",
                        ActParm.FIELD_AC_SQ, ActParm.FIELD_ACOD));
        }catch (Exception e){
            showInfo("Ошибка", e.getMessage());
            throw new IllegalArgumentException("column");
        }
        return sq;
    }

    private void setWrapperFields(){
        wrapper.setAccType(checkLength(checkRequeredString(accType.getValue(), ActParm.FIELD_ACCTYPE), 9, ActParm.FIELD_ACCTYPE));

        checkRequeredString(cusType.getValue(), ActParm.FIELD_CUSTYPE);
        try {
            String s = cusType.getValue().trim();
            if (s.equals("0") || s.equals("00") || s.equals("000")) {
                wrapper.setCusType("00");
            } else{
                Short val = Short.parseShort(s);
                if (val < 1) throw new Exception("Неверный код");
                wrapper.setCusType(val.toString());
            }
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", ActParm.FIELD_CUSTYPE, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        wrapper.setTerm(checkLength(checkRequeredString(term.getValue(), ActParm.FIELD_TERM), 2, ActParm.FIELD_TERM));
        wrapper.setAcc2(checkLength(!_isTech?checkRequeredString(acc2.getValue(), ActParm.FIELD_ACC2):acc2.getValue(), 5, ActParm.FIELD_ACC2));

        wrapper.setPlcode(checkPlCode(plcode.getValue(), wrapper.getAcc2()));

        checkAcc70(wrapper.getAcc2(), wrapper.getPlcode());
        checkAcc706(wrapper.getAcc2(), wrapper.getPlcode());

        wrapper.setAcod(!_isTech ? checkLength(checkRequeredString(acod.getValue(), ActParm.FIELD_ACOD), 4, ActParm.FIELD_ACOD) : acod.getValue());
        wrapper.setAc_sq(!_isTech ? checkAcod_sq(checkLength(checkRequeredString(ac_sq.getValue(), ActParm.FIELD_AC_SQ), 2, ActParm.FIELD_AC_SQ)) : ac_sq.getValue());

        wrapper.setDtb(ClientDateUtils.Date2String(check(dtb.getValue(),
                "Дата начала", "поле не заполнено", new CheckNotNullDate())));
        wrapper.setDte(ClientDateUtils.Date2String(dte.getValue()));

        try {
            if (dte.getValue() != null && dtb.getValue().compareTo(dte.getValue()) == 1) throw new Exception("Дата конца не может быть меньше даты начала");
        }catch (Exception e){
            showInfo("Ошибка", e.getMessage());
            throw new IllegalArgumentException("column");
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            wrapper = new ActParmWrapper();
            setWrapperFields();
            params = wrapper;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

    @Override
    public void afterShow() {
        accType.setFocus(action == FormAction.CREATE && accType.getValue() == null);
        cusType.setFocus(action == FormAction.CREATE && accType.getValue() != null);
        plcode.setFocus(action == FormAction.UPDATE);
    }
}
