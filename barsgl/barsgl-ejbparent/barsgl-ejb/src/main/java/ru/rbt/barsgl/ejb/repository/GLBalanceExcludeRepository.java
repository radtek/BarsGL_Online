package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLBalanceExclude;
import ru.rbt.barsgl.ejb.entity.gl.GLBalanceExcludeId;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
public class GLBalanceExcludeRepository extends AbstractBaseEntityRepository<GLBalanceExclude, GLBalanceExcludeId> {

    /**
     * проверяем наличие счета в исключаемых из расчета баланса
     * по таким счетам, например, синхронизация при обработке проводок не нужна в режиме BUFFER
     * @param bsaacid счет ЦБ
     * @param postingDate дата проводки
     * @return <code>true</code>, если исключен
     */
    public boolean isExcludes(String bsaacid, Date postingDate) {
        return null != selectFirst(GLBalanceExclude.class
                , "from GLBalanceExclude e where e.id.bsaacid = ?1 and ?2 between e.id.dtFrom and e.dtTo", bsaacid, postingDate);
    }
}
