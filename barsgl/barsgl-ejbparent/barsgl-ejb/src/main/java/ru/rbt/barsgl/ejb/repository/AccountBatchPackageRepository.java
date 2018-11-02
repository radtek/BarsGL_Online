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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 17.10.2018.
 */
@Stateless
@LocalBean
public class AccountBatchPackageRepository extends AbstractBaseEntityRepository<AccountBatchPackage, Long> {

    public AccountBatchPackage findById(Long primaryKey) {return refresh(super.findById(AccountBatchPackage.class, primaryKey)); }

    public AccountBatchPackage createAccountPackage(String userName, String fileName, Date curdate, long size) {
        AccountBatchPackage pkg = new AccountBatchPackage();
        pkg.setLoadUser(userName);
        pkg.setFileName(fileName);
        pkg.setOperday(curdate);
        pkg.setCntRequests(size);
        pkg.setState(AccountBatchPackageState.IS_LOAD);

        return save(pkg);
    }

    public boolean checkAccountRequestState(Long packageId, AccountBatchState requestState) throws SQLException {
        DataRecord res = selectFirst("select 1 from GL_ACBATREQ where ID_PKG = ? and STATE <> ?", packageId, requestState.name());
        return null == res;
    }

    public void updateAccountPackageError(AccountBatchPackage pkg, AccountBatchPackageState packageState, String errorMsg) {
        executeNativeUpdate("update GL_ACBATPKG set STATE = ?, INVISIBLE = ?, ERROR_MSG = ? where ID_PKG = ?",
                packageState.name(), YesNo.Y.name(), substr(errorMsg, 4000), pkg.getId());
    }

    public void updateAccountPackageValid(AccountBatchPackage pkg, AccountBatchPackageState packageState, String userProc) {
        executeNativeUpdate("update GL_ACBATPKG set STATE = ?, USER_PROC = ?, TS_STARTV = systimestamp where ID_PKG = ?",
                packageState.name(), userProc, pkg.getId());
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
        return dataList.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    };
}
