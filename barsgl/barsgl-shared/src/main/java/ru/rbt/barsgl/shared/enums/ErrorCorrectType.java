package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.*;

/**
 * Created by ER18837 on 28.02.17.
 */
public enum ErrorCorrectType {
    CLOSE_ONE(NEW), CLOSE_LIST(NEW), REPROCESS_ONE(REPROC), REPROCESS_LIST(REPROC), EDIT_COMMENT(EDIT), EDIT_PST_COMMENT(EDIT);

    public enum CorrectType implements HasLabel{ 
        NEW("Новое сообщение", "помечены закрытыми"),
        REPROC("Переобработка", "отправлены на переобработку"),
        EDIT("Редактирование", "изменены");

        CorrectType(String label, String message) {
            this.label = label;
            this.message = message;
        }

        private String label;
        private String message;

        @Override
        public String getLabel() {
            return label;
        }

        public String getMessage() {
            return message;
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

    public String getTypeLabel() {
        return correctType.label;
    }

    public String getTypeMessage() {
        return correctType.message;
    }

}
