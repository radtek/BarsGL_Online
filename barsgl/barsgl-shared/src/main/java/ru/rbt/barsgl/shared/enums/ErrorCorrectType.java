package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.NEW;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.REPROC;

/**
 * Created by ER18837 on 28.02.17.
 */
public enum ErrorCorrectType {
    CLOSE_ONE(NEW), CLOSE_LIST(NEW), REPROCESS_ONE(REPROC), REPROCESS_LIST(REPROC);

    public enum CorrectType implements HasLabel{ 
        NEW("Новое сообщение"), REPROC("Переобработка");

        CorrectType(String label) {
            this.label = label;
        }

        private String label;
        @Override
        public String getLabel() {
            return label;
        }
    }

    CorrectType correctType;

    ErrorCorrectType(CorrectType correctType) {
        this.correctType = correctType;
    }

    public CorrectType getCorrectType() {
        return correctType;
    }

    public String getTypeName() {
        return correctType.name();
    }

}
