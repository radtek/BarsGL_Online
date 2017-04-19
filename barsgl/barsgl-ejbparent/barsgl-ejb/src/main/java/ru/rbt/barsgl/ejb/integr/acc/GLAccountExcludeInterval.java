package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.shared.Assert;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov
 * Интервал исключения номеров при формировании лицевой части счета
 */
public class GLAccountExcludeInterval implements Serializable {

    private final int startNumber;
    private final int endNumber;

    /**
     * @param startNumber начало
     * @param endNumber окончание
     */
    public GLAccountExcludeInterval(int startNumber, int endNumber) {
        Assert.assertThat(startNumber <= endNumber);
        this.startNumber = startNumber;
        this.endNumber = endNumber;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public int getEndNumber() {
        return endNumber;
    }
}
