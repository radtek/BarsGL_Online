package ru.rbt.barsgl.ejbcore.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class ValidationContext {

    public List<ValidationError> errors = new ArrayList<>();
    public List<DataValidator> validators = new ArrayList<>();

    public void addError(ValidationError error) {
        errors.add(error);
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void addValidator(DataValidator validator) {
        validators.add(validator);
    }

    public List<DataValidator> getValidators() {
        return Collections.unmodifiableList(validators);
    }

    public void validateAll() {
        for (DataValidator validator : validators) {
            try {
                validator.validate();
            } catch (ValidationError error) {
                addError(error);
            }
        }
    }

}
