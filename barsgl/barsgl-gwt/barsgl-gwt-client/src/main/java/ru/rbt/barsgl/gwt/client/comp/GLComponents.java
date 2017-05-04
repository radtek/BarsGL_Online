package ru.rbt.barsgl.gwt.client.comp;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.ColumnsBuilder;
import ru.rbt.barsgl.gwt.core.ui.*;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.PostingType;
import ru.rbt.shared.enums.PrmValueEnum;
import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import ru.rbt.barsgl.gwt.core.comp.Components;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Sort.ASC;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.LONG;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;
import static ru.rbt.barsgl.shared.ClientDateUtils.TZ_CLIENT;

/**
 * Created by ER18837 on 28.10.15.
 */
public class GLComponents {
    public static final char decimalPoint = '.';


    public static TxtBox createTextBoxForSumma(int length, String width)
    {
    	TxtBox res = Components.createTxtBox(length, width);
    	res.addKeyPressHandler(new KeyPressHandler() {

            public void onKeyPress(KeyPressEvent event) {
                char charCode = event.getCharCode();
                String val = ((TextBox) event.getSource()).getText();
                int indPoint = val.indexOf(decimalPoint);
                boolean digitOk = Character.isDigit(charCode) && (indPoint < 0 || val.length() - indPoint <= 2);
                boolean pointOk = (decimalPoint == charCode) && (indPoint < 0);
                if (!digitOk && !pointOk || val.length() >= ((TextBox) event.getSource()).getMaxLength()) {
                    ((TextBox) event.getSource()).cancelKey();
                }
            }
        });


    	return res;
    }

    public static String getSumma(BigDecimal amt) {
        return getSumma(amt, 2);
    }

    public static String getSumma(BigDecimal amt, int scale) {
        if (null == amt)
            return "";
        return new BigDecimal(String.valueOf(amt)).setScale(scale, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * Создает поле выбора источника
     * @return
     */
    public static DataListBox createDealSourceListBox(String src, String width) {
        Columns columns = new ColumnsBuilder().addColumn("ID_SRC", STRING).build();
        DataListBox box = new DataListBox(new ListBoxSqlDataProvider(true, src, "select ID_SRC from GL_SRCPST"
                , columns, null, null, new StringRowConverter()));
        box.setWidth(width);
        return box;
    }

    public static DataListBox createDealSourceAuthListBox(String src, String width){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper != null){
            Columns columns = new ColumnsBuilder().addColumn("ID_SRC", STRING).build();
            DataListBox box = new DataListBox(new ListBoxSqlDataProvider(true, src, Utils.Fmt("select ID_SRC from GL_SRCPST\n" +
                    "where exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = ID_SRC) or\n" +
                    "exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = '*')", wrapper.getId(), PrmValueEnum.Source.name())
                    , columns, null, null, new StringRowConverter()));
            box.setWidth(width);
            return box;
        }
        return createDealSourceListBox(src, width);
    }


    public static DataListBox createCachedDealSourceAuthListBox(String name, String src, String width){
        DataListBox cachedBox = getCachedListBox(name, src);
        if (cachedBox != null) return cachedBox;

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper != null){
            Columns columns = new ColumnsBuilder().addColumn("ID_SRC", STRING).build();
            DataListBox box = new DataListBox(new ListBoxCachedSqlDataProvider(name, true, src, Utils.Fmt("select ID_SRC from GL_SRCPST\n" +
                    "where exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = ID_SRC) or\n" +
                    "exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = '*')", wrapper.getId(), PrmValueEnum.Source.name())
                    , columns, null, null, new StringRowConverter()));
            box.setWidth(width);
            return box;
        }
        return createDealSourceListBox(src, width);
    }

    /**
     * Создает поле выбора бранча из списка
     * @return
     */

    public static DataListBoxEx createBranchListBox(String ccy, String width, boolean withEmptyValue) {
        Columns columns = new ColumnsBuilder().addColumn("BRANCH", LONG).addColumn("NAME", STRING).addColumn("CNUM", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("BRANCH", ASC));

        DataListBoxEx box = new DataListBoxEx(
                new ListBoxSqlDataProvider(withEmptyValue, ccy,
                        "select A8BRCD as BRANCH, A8BRCD || '  ' || A8BRNM as NAME, A8BICN as CNUM from IMBCBBRP"
                        , columns, null, sort, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    public static DataListBoxEx createBranchAuthListBox(String ccy, String width, boolean withEmptyValue){
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper != null) {
            Columns columns = new ColumnsBuilder().addColumn("BRANCH", LONG).addColumn("NAME", STRING)
                    .addColumn("CNUM", STRING).addColumn("CBCCN", STRING).build();
            List<SortItem> sort = new ArrayList<>();
            sort.add(new SortItem("BRANCH", ASC));

            DataListBoxEx box = new DataListBoxEx(
                    new ListBoxSqlDataProvider(withEmptyValue, ccy,
                           Utils.Fmt( "select A8BRCD as BRANCH, A8BRCD || '  ' || A8BRNM as NAME, A8BICN as CNUM, BCBBR as CBCCN from IMBCBBRP\n" +
                                   "where exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = A8CMCD) or\n" +
                                   "exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = '*')" , wrapper.getId(), PrmValueEnum.HeadBranch.name())
                            , columns, null, sort, new StringRowConverter(0, 1)));
            box.setWidth(width);
            return box;
        }

        return createBranchListBox(ccy, width, withEmptyValue);
    }


    /**
     * Создает поле выбора валюты из списка
     * @param ccy
     * @return
     */
    public static DataListBoxEx createCurrencyListBox(String ccy, String width) {
        return createCurrencyListBox(ccy, width, false);
    }

    public static DataListBoxEx createCurrencyListBox(String ccy, String width, boolean numberCode) {
        return createCurrencyListBox(ccy, width, numberCode, false);
    }

    public static DataListBoxEx createCurrencyListBox(String ccy, String width, boolean numberCode, boolean withBlank) {
        Columns columns = new ColumnsBuilder()
        		.addColumn("CCY", STRING).addColumn("CCYN", STRING).addColumn("TEXT", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("CCY", ASC));

        int keyIndex = numberCode ? 1 : 0;
        DataListBoxEx box = new DataListBoxEx(
                new ListBoxSqlDataProvider(withBlank, ccy,
                        "select GLCCY as CCY, CBCCY as CCYN, GLCCY || ' ' || CBCCY as TEXT from CURRENCY c where CYNM IS NOT NULL"
                        , columns, null, sort, new StringRowConverter(keyIndex, 2)));
        box.setWidth(width);
        return box;
    }

    public static DataListBoxEx createCachedCurrencyListBox(String name, String ccy, String width, boolean numberCode, boolean withBlank) {
        DataListBoxEx cachedBox = (DataListBoxEx) getCachedListBox(name, ccy);
        if (cachedBox != null) return cachedBox;

        Columns columns = new ColumnsBuilder().addColumn("CCY", STRING).addColumn("CCYN", STRING).addColumn("TEXT", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("CCY", ASC));

        int keyIndex = numberCode ? 1 : 0;
        DataListBoxEx box = new DataListBoxEx(
                new ListBoxCachedSqlDataProvider(name, withBlank, ccy,
                        "select GLCCY as CCY, CBCCY as CCYN, GLCCY || ' ' || CBCCY as TEXT from CURRENCY c where CYNM IS NOT NULL"
                        ,columns, null, sort, new StringRowConverter(keyIndex, 2)));
        box.setWidth(width);
        return box;
    }

    /**
     * Создает поле выбора филиала
     * @param filial
     * @return
     */
    public static DataListBoxEx createFilialListBox(String filial, String width) {
        return createFilialListBox(filial, width, false, true);
    }

    public static DataListBoxEx createFilialListBox(String filial, String width, boolean numberCode) {
        return createFilialListBox(filial, width, numberCode, true);
    }

    public static DataListBoxEx createFilialListBox(String filial, String width, boolean numberCode, boolean withEmptyValue) {
        Columns columns = new ColumnsBuilder()
                .addColumn("CBCC", STRING).addColumn("CBCCN", STRING).addColumn("TEXT", STRING).build();

        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("CBCC", ASC));

        int keyIndex = numberCode ? 1 : 0;
        DataListBoxEx box = new DataListBoxEx(new ListBoxSqlDataProvider(withEmptyValue, filial,
                "select CCPCD as CBCC, CCBBR as CBCCN, CCPCD || ' ' || CCBBR as TEXT from IMBCBCMP"
                , columns, null, sort, new StringRowConverter(keyIndex, 2)));
        box.setWidth(width);
        return box;
    }

    public static DataListBoxEx createCachedFilialListBox(String name, String filial, String width, boolean numberCode, boolean withEmptyValue) {
        DataListBoxEx cachedBox = (DataListBoxEx) getCachedListBox(name, filial);
        if (cachedBox != null) return cachedBox;

        Columns columns = new ColumnsBuilder()
                .addColumn("CBCC", STRING).addColumn("CBCCN", STRING).addColumn("TEXT", STRING).build();

        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("CBCC", ASC));

        int keyIndex = numberCode ? 1 : 0;
        DataListBoxEx box = new DataListBoxEx(new ListBoxCachedSqlDataProvider(name, withEmptyValue, filial,
                "select CCPCD as CBCC, CCBBR as CBCCN, CCPCD || ' ' || CCBBR as TEXT from IMBCBCMP"
                , columns, null, sort, new StringRowConverter(keyIndex, 2)));
        box.setWidth(width);
        return box;
    }

    public static DataListBoxEx createFilialAuthListBox(String filial, String width, boolean numberCode, boolean withEmptyValue) {
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper != null) {
            Columns columns = new ColumnsBuilder()
                    .addColumn("CBCC", STRING).addColumn("CBCCN", STRING).addColumn("TEXT", STRING).build();

            List<SortItem> sort = new ArrayList<>();
            sort.add(new SortItem("CBCC", ASC));

            int keyIndex = numberCode ? 1 : 0;
            DataListBoxEx box = new DataListBoxEx(new ListBoxSqlDataProvider(withEmptyValue, filial,
                    Utils.Fmt("select CCPCD as CBCC, CCBBR as CBCCN, CCPCD || ' ' || CCBBR as TEXT from IMBCBCMP\n" +
                              "where exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = CCPCD) or\n" +
                              "exists(select 1 from GL_AU_PRMVAL where id_user = {0} and prm_code = '{1}' and prmval = '*')" , wrapper.getId(), PrmValueEnum.HeadBranch.name())
                    , columns, null, sort, new StringRowConverter(keyIndex, 2)));
            box.setWidth(width);
            return box;
        }

        return createFilialListBox(filial, width, numberCode, withEmptyValue);
    }

    /**
     * Создает поле выбора кода срока
     * @return
     */
    public static DataListBox createTermListBox(String term, String width, boolean withEmptyValue) {
        Columns columns = new ColumnsBuilder().addColumn("TERM", STRING).addColumn("NAME", STRING).build();
        DataListBox box = new DataListBox(new ListBoxSqlDataProvider(withEmptyValue, term,
                "select TERM, TERM || ':  ' || TERMNAME as NAME from GL_DICTERM"
                , columns, null, null, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    /**
     * Создает поле выбора типа собственности
     * @return
     */
    public static DataListBox createCustTypeListBox(String ctype, String width, boolean withEmptyValue) {
        String sql = "select CUSTYPE, TYPENAME, CTYPE from V_GL_CBCTP";

        Columns columns = new ColumnsBuilder().addColumn("CUSTYPE", STRING).addColumn("TYPENAME", STRING).addColumn("CTYPE", LONG).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("CTYPE", ASC));

        DataListBox box = new DataListBox(
                new ListBoxSqlDataProvider(withEmptyValue, ctype, sql, columns, null, sort, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    /**
     * Создает поле выбора подразделения
     * @return
     */
    public static DataListBox createDepartmentListBox(String dept, String width, boolean withEmptyValue) {
        Columns columns = new ColumnsBuilder().addColumn("DEPID", STRING).addColumn("NAME", STRING).build();
        DataListBox box = new DataListBox(new ListBoxSqlDataProvider(withEmptyValue, dept,
                "select DEPID, DEPID || '  ' || DEPNAME as NAME from GL_DEPT"
                , columns, null, null, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    public static DataListBox createCachedDepartmentListBox(String name, String dept, String width, boolean withEmptyValue) {
        DataListBox cachedBox = getCachedListBox(name, dept);
        if (cachedBox != null) return cachedBox;

        Columns columns = new ColumnsBuilder().addColumn("DEPID", STRING).addColumn("NAME", STRING).build();
        DataListBox box = new DataListBox(new ListBoxCachedSqlDataProvider(name, withEmptyValue, dept,
                "select DEPID, DEPID || '  ' || DEPNAME as NAME from GL_DEPT"
                , columns, null, null, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    /**
     * Создает поле выбора подразделения
     * @return
     */

    public static DataListBox createProfitCenterListBox(String dept, String width) {
        Columns columns = new ColumnsBuilder().addColumn("PRFCODE", STRING).addColumn("NAME", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("PRFCODE", ASC));
        DataListBox box = new DataListBox(new ListBoxSqlDataProvider(true, dept,
                "select PRFCODE, PRFCODE || ' ' || PRFNAME as NAME from GL_PRFCNTR"
                , columns, null, sort, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    public static DataListBox createCachedProfitCenterListBox(String name, String dept, String width) {
        DataListBox cachedBox = getCachedListBox(name, dept);
        if (cachedBox != null) return cachedBox;

        Columns columns = new ColumnsBuilder().addColumn("PRFCODE", STRING).addColumn("NAME", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("PRFCODE", ASC));
        DataListBox box = new DataListBox(new ListBoxCachedSqlDataProvider(name, true, dept,
                "select PRFCODE, PRFCODE || ' ' || PRFNAME as NAME from GL_PRFCNTR"
                , columns, null, sort, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    public static <T extends Enum> HashMap<Serializable, String> getEnumValuesList(T[] values){
        HashMap<Serializable, String> list = new HashMap<Serializable, String>();
        for (T value: values){
            list.put(value.name(), value.name());
        }
        return list;
    }

    public static <T extends Enum & HasLabel> HashMap<Serializable, String> getEnumLabelsList(T[] values){
        HashMap<Serializable, String> list = new HashMap<Serializable, String>();
        for (T value: values){
            list.put(value.name(), value.name()+ " (" + value.getLabel() + ")");
        }
        return list;
    }

    public static <T> HashMap<Serializable, String> getArrayValuesList(T[] values){
        HashMap<Serializable, String> list = new HashMap<Serializable, String>();
        for (T value: values){
            list.put(value.toString(), value.toString());
        }
        return list;
    }

    public static <T extends Enum & HasLabel> HashMap<Serializable, String> getArrayLabelsList(T[] values, boolean withEmpty){
        HashMap<Serializable, String> list = new HashMap<Serializable, String>();
        if (withEmpty)
            list.put("", "");
        for (T value: values){
            list.put(value.toString(), value.name()+ " (" + value.getLabel() + ")");
        }
        return list;
    }

    public static HashMap<Serializable, String> getPostingTypeList(){
        HashMap<Serializable, String> list = new HashMap<Serializable, String>();
        for (PostingType postingType: PostingType.values()){
            list.put(postingType.getValue(), postingType.getValue() + " (" + postingType.name() + ")");
        }
        return list;
    }

    public static HashMap<Serializable, String> getYesNoList() {
        return getArrayValuesList(new String[]{"", "N", "Y"});
    }

    private static DataListBox getCachedListBox(String name, String selectedValue){
        DataListBox cachedBox = (DataListBox) LocalDataStorage.getParam(Utils.Fmt(ListBoxCachedSqlDataProvider.CACHED_LIST_PREFIX, name));
        if (cachedBox != null){
            cachedBox.setValue(selectedValue);
            cachedBox.setEnabled(true);
            cachedBox.setReadOnly(false);
            //System.out.println("Get cached: " + name);
        }
        return cachedBox;
    }

    public static BtnTxtBox createBtnTextBoxForSumma(int length, String width, Image img, String hint, final ICallMethod cm)
    {
        BtnTxtBox box = new BtnTxtBox(){
            @Override
            public void onBntClick(){
                if (cm != null) cm.method();
            }
        };
        box.setMaxLength(length);
        box.setVisibleLength(length);
        box.setWidth(width);
        box.setButtonImage(img);
        box.setHint(hint);


        box.addKeyPressHandler(new KeyPressHandler() {

            public void onKeyPress(KeyPressEvent event) {
                char charCode = event.getCharCode();
                String val = ((TextBox) event.getSource()).getText();
                int indPoint = val.indexOf(decimalPoint);
                boolean digitOk = Character.isDigit(charCode) && (indPoint < 0 || val.length() - indPoint <= 2);
                boolean pointOk = (decimalPoint == charCode) && (indPoint < 0);
                if (!digitOk && !pointOk || val.length() >= ((TextBox) event.getSource()).getMaxLength()) {
                    ((TextBox) event.getSource()).cancelKey();
                }
            }
        });

        return box;
    }
}
