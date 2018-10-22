package ru.rbt.barsgl.shared.operation;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;

import java.io.Serializable;

/**
 * Created by er18837 on 22.10.2018.
 */
public class AccountBatchWrapper implements Serializable, IsSerializable {
    public enum AccountBatchAction {OPEN, DELETE};

    private Long packageId;
    private AccountBatchPackageState packageState;
    private AccountBatchAction action;

    private Long userId;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public AccountBatchAction getAction() {
        return action;
    }

    public void setAction(AccountBatchAction action) {
        this.action = action;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AccountBatchPackageState getPackageState() {
        return packageState;
    }

    public void setPackageState(AccountBatchPackageState packageState) {
        this.packageState = packageState;
    }
}
