/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.swing.text.html.Option;

import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;

import java.util.Optional;

import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.ACCOUNTING_TYPE_NOT_FOUND;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountingTypeRepository extends AbstractBaseEntityRepository<AccountingType, String> {

    @Override
    public AccountingType findById(Class<AccountingType> clazz, String primaryKey) {
        return Optional.ofNullable(super.findById(clazz, primaryKey))
                .orElseThrow(() -> new ValidationError(ACCOUNTING_TYPE_NOT_FOUND, primaryKey));
    }
}
