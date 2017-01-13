/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.TypesOfTerms;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 *
 * @author Andrew Samsonov
 */
public class TypesOfTermsRepository extends AbstractBaseEntityRepository<TypesOfTerms, String> {
    public  boolean isTermExists(String term) {
        return null != selectFirst(TypesOfTerms.class, "from TypesOfTerms T where T.id = ?1", term);
    }
}
