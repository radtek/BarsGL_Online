package ru.rbt.barsgl.ejbcore.validation;

import ru.rbt.ejbcore.validation.ValidationError;

/**
 * Created by Ivan Sevastyanov
 */
public interface DataValidator {

    void validate() throws ValidationError;

}
