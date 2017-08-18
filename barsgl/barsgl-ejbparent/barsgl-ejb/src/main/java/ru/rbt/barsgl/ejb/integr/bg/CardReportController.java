package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;

/**
 * Created by er18837 on 18.08.2017.
 */
public class CardReportController {

    // TODO заглушка
    public RpcRes_Base<CardReportWrapper> getCardReport(CardReportWrapper wrapper) throws Exception {
        return new RpcRes_Base<>(wrapper, false, "");
    }
}
