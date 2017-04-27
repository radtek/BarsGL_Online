package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.card.CardPst;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by ER22317 on 23.09.2016.
 */
@Stateless
@LocalBean
public class CardPostingRepository extends AbstractBaseEntityRepository<CardPst, Long> {
}
