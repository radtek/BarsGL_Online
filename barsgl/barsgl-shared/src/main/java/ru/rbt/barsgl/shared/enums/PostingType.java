package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 05.04.16.
 */
public enum PostingType {
    OneFilial("1"),
    ExchDiff("2"),
    MfoDebit("3"),
    MfoCredit("4"),
    FanMain("5");

    private final String value;

    private PostingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public static PostingType parseType(String value) {
        for (PostingType postingType : values()) {
            if (postingType.getValue().equals(value)) return postingType;
        }
        return null;
    }
}

