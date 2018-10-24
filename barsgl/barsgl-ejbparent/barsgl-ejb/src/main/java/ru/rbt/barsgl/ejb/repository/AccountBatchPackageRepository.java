package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by er18837 on 17.10.2018.
 */
@Stateless
@LocalBean
public class AccountBatchPackageRepository extends AbstractBaseEntityRepository<AccountBatchPackage, Long> {

    public AccountBatchPackage findById(Long primaryKey) {return refresh(super.findById(AccountBatchPackage.class, primaryKey)); }

    public boolean checkAccountRequestState(Long packageId, AccountBatchState requestState) throws SQLException {
        DataRecord res = selectFirst("select 1 from GL_ACBATREQ where ID_PKG = ? and STATE <> ?", packageId, requestState.name());
        return null == res;
    }

    public void updateAccountPackageState(AccountBatchPackage pkg, AccountBatchPackageState packageState, String userProc) {
        executeNativeUpdate("update GL_ACBATPKG set STATE = ?, USER_PROC = ?, TS_STARTV = systimestamp where ID_PKG = ?", packageState.name(), userProc, pkg.getId());
    };

    public void deleteAccountPackageOther(AccountBatchPackage pkg, AccountBatchPackageState packageState, String userProc) {
        executeNativeUpdate("update GL_ACBATPKG set INVISIBLE = ?, USER_PROC = ?, TS_ENDP = systimestamp where ID_PKG = ? and STATE = ?",
                YesNo.Y.name(), userProc, pkg.getId(), packageState.name());
    };

    public void deleteAccountPackageOwn(AccountBatchPackage pkg, AccountBatchPackageState packageState) {
        executeNativeUpdate("delete from GL_ACBATREQ where ID_PKG = ? ", pkg.getId());
        executeNativeUpdate("delete from GL_ACBATPKG where ID_PKG = ? and STATE = ?", pkg.getId(), packageState.name());
    };

    public boolean checkFilialPermission(Long userId, String filial) throws Exception {
        DataRecord data = selectFirst("select 1 from GL_AU_PRMVAL where ID_USER = ? and PRM_CODE = ? and PRMVAL in (?, ?)",
                userId, "HeadBranch", "*", filial);

        return (null == data);
    }

    public List<String> getAllowedBranches(Long user_id) throws SQLException {
        List<String> list = new ArrayList<>();
        DataRecord data = selectFirst("select prmval from GL_AU_PRMVAL where ID_USER = ? and PRM_CODE = 'HeadBranch'", user_id);
        if (null == data)
            return list;

        String filial = data.getString(0);
        if ("*".equals(filial)) {
            list.add("*");
            return list;
        }

        List<DataRecord> dataList = select("select A8BRCD from IMBCBBRP where A8CMCD = ?", filial);
        dataList.stream().map(r -> list.add(r.getString(0)));
        return list;
    };
}
