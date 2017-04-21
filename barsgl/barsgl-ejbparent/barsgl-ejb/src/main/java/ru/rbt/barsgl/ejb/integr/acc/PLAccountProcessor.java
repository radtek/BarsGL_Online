package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

import javax.inject.Inject;

import static ru.rbt.ejbcore.validation.ErrorCode.ACCOUNT_707_BAD_BRANCH;

/**
 * Created by ER18837 on 03.12.16.
 */
public class PLAccountProcessor extends ValidationAwareHandler<AccountKeys> {
    @Inject
    private GLAccountRepository glAccountRepository;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Override
    public void fillValidationContext(AccountKeys target, ValidationContext context) {
        glAccountProcessor.fillValidationContext(target, context);

        // Бранч
        context.addValidator(() -> {
            String fieldName = "Отделение";
            if ("707".equals(StringUtils.substr(target.getAccount2(), 0, 3)) && !"001".equals(target.getBranch())) {
                throw new ValidationError(ACCOUNT_707_BAD_BRANCH, fieldName);
            }
        });

    }

    public AccountKeys fillAccountKeysMidas(ManualAccountWrapper accountWrapper, AccountKeys keys) {
        keys.setAccountCode(accountWrapper.getAccountCode().toString());
        keys.setAccSequence(String.format("%02d", accountWrapper.getAccountSequence()));
        // счет Майдас
        String acid = glAccountRepository.makeMidasAccount(
                Integer.parseInt(accountWrapper.getCustomerNumber()),
                keys.getCurrency(),
                keys.getBranch(),
                accountWrapper.getAccountCode(),
                accountWrapper.getAccountSequence()
        );
        keys.setAccountMidas(acid);

        return keys;
    }

}
