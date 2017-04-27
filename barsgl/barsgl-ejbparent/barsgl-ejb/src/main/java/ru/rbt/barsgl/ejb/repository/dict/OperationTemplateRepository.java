package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.OperationTemplate;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.sql.SQLException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by ER18837 on 17.03.16.
 */
@Stateless
@LocalBean
public class OperationTemplateRepository extends AbstractBaseEntityRepository<OperationTemplate, Long> {

    public boolean checkTemplateNameExists(String name, Long id) {
        if (null == id)
            return null != selectFirst(OperationTemplate.class, "from OperationTemplate T where T.templateName = ?1", name);
        else
            return null != selectFirst(OperationTemplate.class, "from OperationTemplate T where T.templateName = ?1 and T.id <> ?2",
                    name, id);
    }

    public boolean checkAccount2Exists(String acc2Pattern) {
        try {
            DataRecord res = selectFirst("select count(1) from BSS B where B.ACC2 like ?", acc2Pattern);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord getAccountParams(String bsaAcid) {
        try {
            String sql = "select B.ID, I.CCPCD as CBCC, C.GLCCY as CCY, B.BSAACO, B.BSAACC " +
                    "from BSAACC B join IMBCBCMP I on I.CCBBR = B.BRCA join CURRENCY C on B.CCY = C.CBCCY where B.ID = ?";
            DataRecord res = selectFirst(sql, bsaAcid);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
