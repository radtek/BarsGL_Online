package ru.rbt.barsgl.ejb.integr.acc;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 * Тип счетчика
 */
public enum GLAccountCounterType {
    /**
     * Счета активов и пассивов
     */
    ASSET_LIABILITY("00000000", 20_000_001
            , new GLAccountExcludeInterval(30_000_000, 40_000_000)
            , new GLAccountExcludeInterval(50_000_000, 60_000_000));
    /**
     * Счета доходов и расходов
     * @deprecated используется утилита ru.rb.ucb.util.AccountUtil
     */
    /*PROFIT_LOSS("0000", 2001
            , new GLAccountExcludeInterval(3000, 4000)
            , new GLAccountExcludeInterval(5000, 6000)
    );*/

    private final DecimalFormat decimalFormat ;
    private final int startNumber;
    private final String decimalFormatString;
    private final List<GLAccountExcludeInterval> excludes;

    GLAccountCounterType(String decimalFormatString, int startNumber, GLAccountExcludeInterval ... excludes) {
        this.decimalFormatString = decimalFormatString;
        this.decimalFormat = new DecimalFormat(decimalFormatString);
        this.startNumber = startNumber;
        if (null != excludes) {
            this.excludes = Arrays.asList(excludes);
        } else {
            this.excludes = Arrays.asList();
        }
    }

    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public String getDecimalFormatString() {
        return decimalFormatString;
    }

    public List<GLAccountExcludeInterval> getExcludes() {
        return excludes;
    }
}
