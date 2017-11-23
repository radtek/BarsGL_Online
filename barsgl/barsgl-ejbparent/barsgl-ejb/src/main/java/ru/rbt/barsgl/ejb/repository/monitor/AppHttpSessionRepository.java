package ru.rbt.barsgl.ejb.repository.monitor;

import ru.rbt.barsgl.ejb.common.controller.od.DataBaseTimeService;
import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.inject.Inject;

/**
 * Created by Ivan Sevastyanov on 14.11.2017.
 */
public class AppHttpSessionRepository extends AbstractBaseEntityRepository<AppHttpSession, Long> {

    @Inject
    private DataBaseTimeService timeService;

    public int pingSession(String sessionId) {
        return executeUpdate("update AppHttpSession s set s.lastAccessTime = ?1 where s.sessionId = ?2"
                , timeService.getCurrentTime(), sessionId);
    }
}
