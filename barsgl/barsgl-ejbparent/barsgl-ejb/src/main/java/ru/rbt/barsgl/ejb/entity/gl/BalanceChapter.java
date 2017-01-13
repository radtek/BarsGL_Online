package ru.rbt.barsgl.ejb.entity.gl;

/**
 * Created by Ivan Sevastyanov
 * Глава баланса
 */
public enum BalanceChapter {
    A("А"),
    B("Б"),
    V("В"),
    G("Г"),
    D("Д");
    private String ruLetter;

    private BalanceChapter(String ruLetter) {
        this.ruLetter = ruLetter;
    }

    public String getRuLetter() {
        return ruLetter;
    }

    public static BalanceChapter parseChapter(String ruLetter) {
        for (BalanceChapter chapter : values()) {
            if (chapter.getRuLetter().equals(ruLetter)) return chapter;
        }
        return null;
    }
}
