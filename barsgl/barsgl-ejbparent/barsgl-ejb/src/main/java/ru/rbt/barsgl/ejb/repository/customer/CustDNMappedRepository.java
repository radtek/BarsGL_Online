package ru.rbt.barsgl.ejb.repository.customer;

import ru.rbt.barsgl.ejb.entity.cust.CustDNMapped;
import ru.rbt.barsgl.ejb.entity.cust.Customer;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.repository.BaseEntityRepository;
import ru.rbt.shared.Assert;

import javax.persistence.NoResultException;
import java.sql.SQLException;

/**
 * Created by er18837 on 18.12.2017.
 */
public class CustDNMappedRepository extends AbstractBaseEntityRepository<CustDNMapped,Long> {

    public String getCustTypeByFccType(String fcType) throws SQLException {
        try {
            DataRecord dataRecord = selectOne("select CCCTMD from IFXCCCTP where CCCTFC = ?", fcType);
            return dataRecord.getString(0);
        } catch (NoResultException e) {
            return null;
        }
    }

    public void updateResult(Long jId, CustDNMapped.CustResult result) {
        int cnt = executeUpdate("update CustDNMapped cm set cm.result = ?1 where cm.id = ?2", result, jId);
        Assert.isTrue(cnt == 1, "Не удалось обновить запись id = " + jId + " в GL_CUDENO3");
    }

    public CustDNMapped updateOldFields(Long jId, Customer customer, CustDNMapped.CustResult result) {
        CustDNMapped mapped = findById(CustDNMapped.class, jId);
        mapped.setBranchOld(customer.getBranch());
        mapped.setResidentOld(customer.getResident());
        mapped.setCbTypeOld(customer.getCbType());
        mapped.setResult(result);
        return update(mapped);
    }

}
