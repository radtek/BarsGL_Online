package ru.rbt.barsgl.ejb.repository.monitor;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov on 14.11.2017.
 */
public class AppSessionListenerRepository extends AbstractBaseEntityRepository<AppHttpSession, Long> {

    public int invalidateSession(String sessionId) {
        return executeUpdate("update AppHttpSession s set s.invalidated = ?1 where s.sessionId = ?2"
                , YesNo.Y, sessionId);
    }
}
