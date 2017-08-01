package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.List;

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

    /**
     * Определяет филиал, в котором открыт счет
     *
     * @param bsaAcid - номер счета ЦБ
     * @return - филиал (3 буквы)
     * @throws SQLException
     */
    public String getFilialByAccount(String bsaAcid) {
        try {
            String sql = "select CBCC from GL_ACC where BSAACID = ?";
            DataRecord res = selectFirst(sql, bsaAcid);
            return (null != res) ? res.getString("CBCC") : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Подавляет проводки в Pd
     *
     * @param invisible true - подавить
     * @param postings  список проводок, которые надо подавить
     */
    public int updatePdInvisible(boolean invisible, List<GLPosting> postings) {

        if (postings.isEmpty()) {
            return 0;
        }

        String strInvisible = invisible ? "1" : "0";
        StringBuilder pcidIn = new StringBuilder();
        pcidIn.append("update GL_PDTH set invisible = ? where PDID in (");
        for (GLPosting posting : postings) {
            pcidIn.append(posting.getId()).append(",");
        }
        pcidIn.setCharAt(pcidIn.length() - 1, ')');
        String sql = pcidIn.toString();
        return executeNativeUpdate(sql, strInvisible);
    }
}
