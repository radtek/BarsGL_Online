package ru.rbt.barsgl.ejb.integr;

import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;

import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class ValidationAwareHandler<T> {

    /**
     * валидация данных операции перед обработкой
     * @param target проверяемый объект
     * @param context контекс
     * @return ошибки валидации
     */
    public final List<ValidationError> validate(T target, ValidationContext context) {
        fillValidationContext(target, context);
        context.validateAll();
        return context.getErrors();
    }

    /**
     * Заполняем контекст
     * @param target проверяемый объект
     * @param context контекс
     */
    public abstract void fillValidationContext(T target, ValidationContext context);

    public final static String validationErrorsToString(List<ValidationError> errors) {
//        String res1 = Joiner.on(",").join(errors);
        StringBuilder result = new StringBuilder();
        for (ValidationError error : errors) {
            result.append(error.getMessage());
        }
        return result.toString();
    }

    public final static String validationErrorsToString(List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder();
        for (ValidationError error : errors) {
            String errCode = ValidationError.getErrorCode(error.getMessage());
            String errMessage = ValidationError.getErrorText(error.getMessage());
            if (null != descriptors) {
                descriptors.addErrorDescription(errMessage, errCode);
            }
            result.append(error.getMessage());
        }
        return result.toString();
    }

}

