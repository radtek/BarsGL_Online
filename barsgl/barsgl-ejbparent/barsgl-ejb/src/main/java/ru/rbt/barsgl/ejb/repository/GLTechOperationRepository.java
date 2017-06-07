package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;

import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by er23851 on 02.06.2017.
 */
@Stateless
@LocalBean
public class GLTechOperationRepository extends AbstractBaseEntityRepository<GLOperation, Long>
{
    public Long getStornoOperationID(GLOperation operation) throws SQLException {

        String sql = "select GLOID from GL_OPER where EVT_ID = ?" +
                " and VALUE(DEAL_ID, '') = ? and VALUE(PMT_REF, '') = ?" +                // TODO проверка на null
                " and VDATE = ? and STATE in ('LOAD', 'POST', 'WTAC') " +
                " and ACCKEY_DR = ? and AMT_DR = ? and ACCKEY_CR = ? and AMT_CR = ? ";

        //and STATE in ('LOAD', 'POST', 'WTAC')
//        String dealId = (null == operation.getDealId()) ? "" : operation.getDealId();
//        String paymentRefernce = (null == operation.getPaymentRefernce()) ? "" : operation.getPaymentRefernce();
        DataRecord res = selectFirst(sql,
                operation.getStornoReference(),
                ifEmpty(operation.getDealId(), ""),             //operation.getDealId(),
                ifEmpty(operation.getPaymentRefernce(), ""),    //operation.getPaymentRefernce(),
                operation.getValueDate(),

                operation.getAccountKeyCredit(), operation.getAmountCredit(),
                operation.getAccountKeyDebit(), operation.getAmountDebit());

        return (null != res) ? res.getLong(0) : null;
    }
}
