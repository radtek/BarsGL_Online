package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLBsaAccLock;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class GLBsaAccLockRepository extends AbstractBaseEntityRepository<GLBsaAccLock, String> {

    @EJB
    private PdRepository pdRepository;

    /**
     * Создать/обновить запись о блокировке счета
     * @param lock
     */
    public void createOrUpdateLock(GLBsaAccLock lock) throws Exception {
        int cnt = executeNativeUpdate("update GL_BSACCLK set UPD_DATE = ? where BSAACID = ?"
                , lock.getUpdateDate(), lock.getBsaAcid());
        if (0 == cnt) {
            executeNativeUpdate("insert into GL_BSACCLK (BSAACID, UPD_DATE) values (?1,?2)"
                    , lock.getBsaAcid(), lock.getUpdateDate());
        }
    }

}
