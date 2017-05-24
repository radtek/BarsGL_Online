package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.enterprise.inject.Alternative;
import java.util.List;

/**
 * Created by er21006 on 23.05.2017.
 */
@Alternative
public class MovementCreateProcessorAlt implements MovementCommunicator {

    @Override
    public void sendRequests(List<MovementCreateData> mcdList) {
        throw new DefaultApplicationException("Not implemented sending");
    }
}
