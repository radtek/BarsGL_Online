package ru.rbt.barsgl.ejb.integr.acc;

import ru.rb.ucb.util.AccountUtil;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.*;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.shared.Assert.isTrue;

/**
 * Created by Ivan Sevastyanov
 */
public class GLAccountFrontPartController {

    private static final String FACTOR = "713713713713713713713713713";

    /*private static final Map<String,Integer> accNumSymbs = AccountNumberSymbolsBuilder
            .create()
            .addEntry("0").addEntry("1").addEntry("2")
            .addEntry("3").addEntry("4").addEntry("5")
            .addEntry("6").addEntry("7").addEntry("8")
            .addEntry("9").addEntry("A").addEntry("K").build();*/

    private static final Map<String,Integer> accNumSymbs = new HashMap<String,Integer>(){{
        put("0",0);
        put("1",1);
        put("2",2);
        put("3",3);
        put("4",4);
        put("5",5);
        put("6",6);
        put("7",7);
        put("8",8);
        put("9",9);

        // Латиница
        put("A",0);
        put("B",1);
        put("C",2);
        put("E",3);
        put("H",4);
        put("K",5);
        put("M",6);
        put("P",7);
        put("T",8);
        put("X",9);

        // Кириллица
        put("А",0);
        put("В",1);
        put("С",2);
        put("Е",3);
        put("Н",4);
        put("К",5);
        put("М",6);
        put("Р",7);
        put("Т",8);
        put("Х",9);
    }};

    /* Из SQL функции. Второй комплект букв видимо кириллица
            when @C6 in ('A', 'А') THEN 0
            when @C6 in ('B', 'В') THEN 1
            when @C6 in ('C', 'С') THEN 2
            when @C6 in ('E', 'Е') THEN 3
            when @C6 in ('H', 'Н') THEN 4
            when @C6 in ('K', 'К') THEN 5
            when @C6 in ('M', 'М') THEN 6
            when @C6 in ('P', 'Р') THEN 7
            when @C6 in ('T', 'Т') THEN 8
            when @C6 in ('X', 'Х') THEN 9
     */

    @EJB
    private GLAccountRepository repository;

    /**
     * Получение следующего номера лицевой части счета ЦБ по набору параметров
     * @param acc2
     * @param currencyCodeAlpha
     * @param companyCode
     * @param plCode
     * @return
     */
    public String getNextFrontPartNumber(String acc2, String currencyCodeAlpha, String companyCode, String plCode) {
        List<String> params = Arrays.asList(acc2, currencyCodeAlpha
                , companyCode, ifEmpty(plCode, "[empty]"));
        isTrue(params.stream().anyMatch(target -> !isEmpty(target)), format("Неверный набор параметров: <%s>", params.stream().collect(joining(","))));
        try {
            return repository.executeInNewTransaction(persistence -> next(acc2, currencyCodeAlpha, companyCode, plCode));
        } catch (Exception e) {
            try {
                // второй раз в текущей транзакции для автоматического отката в случае ошибки
                return next(acc2, currencyCodeAlpha, companyCode, plCode);
            } catch (Exception e1) {
                throw new DefaultApplicationException(e1.getMessage(), e1);
            }
        }
    }

    /**
     * Расчёт ключевого разряда
     * @param account 20-ти значный счет. В 9-м разряде допускается любой символ
     * @param cbccn код компании
     * @return 20-ти значный счет с расчитанным ключевым разрядом
     */
    public String calculateKeyDigit(String account, String cbccn) {
        isTrue(!isEmpty(account) && account.matches("\\d{5}.{4}\\d{11}")
                , format("Неверный счет '%s'. Ожидается в формате \\d{8}.\\d{11}", account));
        final String leftPart = account.substring(0, 8);
        final String rightPart = account.substring(9);
        String tempAccount = leftPart.concat("0").concat(rightPart);
        int sum = 0;
        final String bankBic = getClientBic(cbccn);
        sum += calcAccountCoefMulti(bankBic, tempAccount);
        sum = sum*3%10;
        String key = Integer.toString(sum).trim();
        Assert.isTrue(1 == key.length(), "Не удалось рассчитать ключевой разряд");
        String resultAccount = leftPart.concat(key).concat(rightPart);
        check(bankBic, resultAccount);
        return resultAccount;
    }

    /**
     *
     * @param cbccn код компании
     * @return БИК клиента
     */
    private String getClientBic(String cbccn) {
        try {
            return Optional.ofNullable(repository.selectFirst(
                    "select r.bxbicc \n" +
                    "  from sdcustpd r\n" +
                    " where r.bbcust = (select t.a8bicn \n" +
                    "                      from imbcbbrp t\n" +
                    "                     where t.br_head ='Y'\n" +
                    "                       and t.bcbbr = ?)", cbccn))
                    .orElseThrow(() -> new DefaultApplicationException(format("Не найден БИК для клиента '%s'", cbccn))).getString("bxbicc");
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private int getAccDigit2Number(final char digit, String account) {
        Integer result = accNumSymbs.get(new String(new char[]{digit}));
        if (null != result) {
            return result;
        } else {
            throw new IllegalArgumentException(format("Недопустимый символ '%s' в номере аналитического счета '%s'", digit, account));
        }
    }

    private String next(String acc2, String currencyCodeAlpha, String companyCode, String plCode) throws Exception {
        if (isEmpty(plCode)) {
            // блокировка
            int cnt = repository.
                    executeNativeUpdate("update GL_ACNOCNT set COUNT = COUNT where ACC2 = ? and  CCYN = ? and CBCCN = ? and PLCOD IS NULL",
                            requiredNotEmpty(acc2, "empty acc2"), requiredNotEmpty(currencyCodeAlpha, "empty currencyCodeAlpha")
                            , requiredNotEmpty(companyCode, "empty companyCode"));
            GLAccountCounterType type = GLAccountCounterType.ASSET_LIABILITY;
            String result;
            if (0 == cnt) {
                repository.executeNativeUpdate("insert into GL_ACNOCNT (ACC2, CCYN, CBCCN, PLCOD, COUNT) values (?,?,?,NULL,?)"
                        , acc2, currencyCodeAlpha, companyCode
                        , Integer.toString(type.getStartNumber()));
                result = type.getDecimalFormat().format(type.getStartNumber());
            } else {
                DataRecord record = repository.selectOne("select * from GL_ACNOCNT where ACC2 = ? and  CCYN = ? and CBCCN = ? and PLCOD IS NULL",
                        acc2, currencyCodeAlpha
                        , companyCode);
                int count = getNextNumberExcludesAware(type, record.getInteger("count"));
                cnt = repository.
                        executeNativeUpdate("update GL_ACNOCNT set COUNT = ? where ACC2 = ? and  CCYN = ? and CBCCN = ? and PLCOD IS NULL",
                                count, acc2, currencyCodeAlpha
                                , companyCode);
                Assert.assertThat(1 == cnt);
                result = type.getDecimalFormat().format(count);
            }
            isTrue(result.length() == type.getDecimalFormatString().length()
                    , format("Размер строки '%s' не соответствует длине формата '%s'", result, type.getDecimalFormatString()));
            return result;
        } else {
            // счета доходов/расходов создаем внешней утилитой
            return repository.executeInNonTransaction(conn->
                    AccountUtil.next(conn, acc2, currencyCodeAlpha, companyCode, plCode));
        }

    }

    private int getNextNumberExcludesAware(GLAccountCounterType type, int currentNumber) {
        List<GLAccountExcludeInterval> excludes = type.getExcludes();
        if (excludes.isEmpty()) {
            return currentNumber++;
        } else {
            Collections.sort(excludes, (in1, in2) ->
                    in1.getStartNumber() < in2.getStartNumber() ? -1 : in1.getStartNumber() == in2.getStartNumber() ? 0 : 1);
            Optional<GLAccountExcludeInterval> optional;
            do {
                currentNumber++;
                final int finalCurrentNumber = currentNumber;
                optional = excludes.stream().filter(e -> finalCurrentNumber >= e.getStartNumber()
                        && finalCurrentNumber <= e.getEndNumber()).findFirst();
                if (optional.isPresent()) {
                    currentNumber = optional.get().getEndNumber();
                }
            }
            while (optional.isPresent());
        }
        return currentNumber;
    }

    private void check(String bik, String account) {
        int sum = calcAccountCoefMulti(bik, account);
        Assert.isTrue(0 == sum%10, format("Не прошла проверка ключевого разряда для полученного счета '%s' сумма '%s'", account, sum));
    }

    private int calcAccountCoefMulti(String bik, String account) {
        int sum = 0;
        for (int i = 1; i <= 3; i++) {
            sum += parseInt(bik.substring(5 + i, 6 + i))*parseInt(FACTOR.substring(i - 1, i));
        }
        for (int i = 1; i <= 20; i++) {
            sum += getAccDigit2Number(account.substring(i - 1, i).toCharArray()[0], account)*parseInt(FACTOR.substring(i + 2, i + 3));
        }
        return sum;
    }

/*
    private static class AccountNumberSymbolsBuilder implements Builder<Map<String, Integer>>{

        private HashMap<String, Integer> storage = new HashMap<>();

        public static AccountNumberSymbolsBuilder create() {
            return new AccountNumberSymbolsBuilder();
        }

        @Override
        public Map<String, Integer> build() {
            return storage;
        }

        public AccountNumberSymbolsBuilder addEntry(String key) {
            Assert.assertThat(key.matches("(\\d)|([KA])"));
            if (key.matches("\\d")) {
                storage.put(key, parseInt(key));
            } else {
                storage.put(key, 0);
            }
            return this;
        }
    }
*/


}
