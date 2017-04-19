package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.card.CardXls;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.Date;
import java.util.List;

/**
 * Created by ER22317 on 23.09.2016.
 */
public class CardXlsRepository extends AbstractBaseEntityRepository<CardXls, Long> {
    public List<CardXls> getOkCardByPkg (BatchPackage pkg) {
        String where = " where c.packageId = ?1 and c.ecode='0'";
        return select(CardXls.class, "FROM CardXls c " + where, pkg.getId());
    }

}
