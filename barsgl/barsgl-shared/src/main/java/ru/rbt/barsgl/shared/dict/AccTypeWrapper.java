package ru.rbt.barsgl.shared.dict;

import ru.rbt.barsgl.shared.enums.BoolType;

/**
 * Created by akichigi on 19.08.16.
 */
public class AccTypeWrapper extends AccTypeModifierWrapper {
    private String acctype;
    private String acctypeName;
    private BoolType pl_act;
    private BoolType fl_ctrl;
    private BoolType tech_act;

    public String getAcctype() {
        return acctype;
    }

    public void setAcctype(String acctype) {
        this.acctype = acctype;
    }

    public String getAcctypeName() {
        return acctypeName;
    }

    public void setAcctypeName(String name) {
        this.acctypeName = name;
    }

    public BoolType getPl_act() {
        return pl_act;
    }

    public void setPl_act(BoolType pl_act) {
        this.pl_act = pl_act;
    }

    public BoolType getFl_ctrl() {
        return fl_ctrl;
    }

    public void setFl_ctrl(BoolType fl_ctrl) {
        this.fl_ctrl = fl_ctrl;
    }

    public BoolType getTech_act() {
        return tech_act;
    }

    public void setTech_act(BoolType tech_act) {
        this.tech_act = tech_act;
    }
}
