package ru.rbt.barsgl.ejbcore.validation;

/**
 * Created by Ivan Sevastyanov
 */
public interface DataValidator {

    void validate() throws ValidationError;

}
