package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class EtlPostingRepository extends AbstractBaseEntityRepository<EtlPosting,Long> {

    public List<EtlPosting> getPostingByPackage(EtlPackage etlPackage, YesNo storno) {
        return select(EtlPosting.class, "FROM EtlPosting p WHERE p.etlPackage = ?1 and p.storno = ?2 ORDER BY p.id", etlPackage, storno);
    }

    public String getOperationCurrency(EtlPosting posting, GLOperation.OperSide operSide) {
        String fieldName = (GLOperation.OperSide.D.equals(operSide)) ? "CCY_DR" : "CCY_CR";
        try {
            DataRecord res = selectFirst("select " + fieldName + " from GL_ETLPST where ID = ?", posting.getId());
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void updatePostingStateSuccess(EtlPosting posting) {
        executeUpdate("update EtlPosting p set p.errorCode = ?1, p.errorMessage = ?2 where p = ?3",
                0, "SUCCESS", posting);
    }

    public void updatePostingStateError(EtlPosting posting, String message) {
        executeUpdate("update EtlPosting p set p.errorCode = ?1, p.errorMessage = ?2 where p = ?3",
                1, substr(message, 4000), posting);
    }

    public long nextId() {
        return nextId("GL_SEQ_PST");
    }

    public List<EtlPosting> getFailedPostingForRepsrocessing(EtlPackage etlPackage) {
        // получаем через ... сразу инициализируем ссылку на пакет, иначе потом падает при доступе к пакету в асинхронном вызове
        List<EtlPosting> failedPostings = findNative(EtlPosting.class
                , "select p.*\n" +
                "    from gl_etlpst p\n" +
                "   where p.id_pkg = ?\n" +
                "     and p.id_pst not like '%*'\n" +
                "     and not exists (select 1 from gl_etlpst o where o.id_pst = p.id_pst || '*' and p.id_pkg = o.id_pkg)\n" +
                "     and ( not exists (select 1 from gl_oper o where o.id_pst = p.id_pst and o.state in ('POST', 'LOAD', 'WTAC'))\n" +
                "      or p.ecode <> '0')", 10000, etlPackage.getId());
        List<EtlPosting> allPostings = select(EtlPosting.class, "from EtlPosting p join fetch p.etlPackage k where k.id = ?1", etlPackage.getId());
        return allPostings.stream().filter(failedPostings::contains).collect(Collectors.toList());
    }

}
