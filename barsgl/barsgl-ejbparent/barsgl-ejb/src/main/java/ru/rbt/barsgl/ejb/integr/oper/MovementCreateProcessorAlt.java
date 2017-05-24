package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;

import javax.ejb.EJB;
import javax.enterprise.inject.Alternative;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Created by er21006 on 23.05.2017.
 */
@Alternative
public class MovementCreateProcessorAlt implements MovementCommunicator {

    @EJB
    private AuditController auditController;

    @Override
    public void sendRequests(List<MovementCreateData> mcdList) {
        auditController.info(AuditRecord.LogCode.MovementCreate
                , format("Message list was sent: %s", mcdList.stream().map(MovementCreateData::toString).collect(Collectors.joining(">:<"))));
    }
}
