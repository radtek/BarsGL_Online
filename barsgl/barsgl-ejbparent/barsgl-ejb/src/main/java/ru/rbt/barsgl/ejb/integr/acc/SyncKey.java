package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.shared.Builder;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by Ivan Sevastyanov on 09.10.2018.
 * Ключ для синхронизации при создании счетов
 */
public class SyncKey {

    private final String key;

    private SyncKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static SyncKeyBuilder builder() {
        return new SyncKeyBuilder();
    }

    public static class SyncKeyBuilder implements Builder<SyncKey> {

        private SyncKeyBuilder() {}

        private String branch;

        private String ccy;

        private String custNo;

        private String accType;

        private String cusType;

        private String term;

        public SyncKeyBuilder setBranch(String branch) {
            this.branch = branch;
            return this;
        }

        public SyncKeyBuilder setCcy(String ccy) {
            this.ccy = ccy;
            return this;
        }

        public SyncKeyBuilder setCustNo(String custNo) {
            this.custNo = custNo;
            return this;
        }

        public SyncKeyBuilder setAccType(String accType) {
            this.accType = accType;
            return this;
        }

        public SyncKeyBuilder setCusType(String cusType) {
            this.cusType = ifEmpty(cusType, "00");
            return this;
        }

        public SyncKeyBuilder setTerm(String term) {
            this.term = ifEmpty(term, "00");
            return this;
        }

        public SyncKeyBuilder fromAccountKeys(AccountKeys keys) {
            return setBranch(keys.getBranch())
                    .setCcy(keys.getCurrency())
                    .setCustNo(keys.getCustomerNumber())
                    .setAccType(keys.getAccountType())
                    .setCusType(keys.getCustomerType())
                    .setTerm(keys.getTerm());
        }

        @Override
        public SyncKey build() {
            try {
                return new SyncKey(
                               getField("branch", branch)
                        + ":"+ getField("ccy", ccy)
                        + ":"+ getField("custNo", custNo)
                        + ":"+ getField("accType", accType)
                        + ":"+ getField("cusType", cusType)
                        + ":"+ getField("term", term)
                );
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }
    }

    private static Predicate<String> notEmpty(String input) {
        return s -> !StringUtils.isEmpty(input);
    }

    private static Supplier<Exception> fieldIsEmpty(String name) {
        return () -> new Exception(String.format("Часть ключа блокировки '%s' не установлена", name));
    }

    private static String getField(String name, String input) throws Exception {
        return Assert.assertThat(input, notEmpty(input), fieldIsEmpty(name));
    }
}
