package ru.rbt.barsgl.ejb.integr.oper;

/**
 * Created by Ivan Sevastyanov
 */
public enum PostingDateType {

    /**
     * текущий ОД
     */
    CURRENT,
    /**
     * предыдущий ОД
     */
    LAST,
    /**
     * по стандартному алгоритму
     */
    STANDARD,
    /**
     * жестко задан значением возвращаемым из представления (в т.ч. для заведенных вручную)
     */
    HARD,
}
