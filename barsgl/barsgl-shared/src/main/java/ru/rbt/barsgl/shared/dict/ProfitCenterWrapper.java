package ru.rbt.barsgl.shared.dict;

import ru.rbt.barsgl.shared.enums.BoolType;

/**
 * Created by akichigi on 03.08.16.
 */
public class ProfitCenterWrapper extends CodeNameWrapper {
    private BoolType closed;

    public BoolType getClosed() {
        return closed;
    }

    public void setClosed(BoolType closed) {
        this.closed = closed;
    }
}
