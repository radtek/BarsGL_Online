package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;

import java.util.List;

/**
 * Created by er21006 on 23.05.2017.
 */
public interface MovementCommunicator {
    void sendRequests(List<MovementCreateData> mcdList);
}
