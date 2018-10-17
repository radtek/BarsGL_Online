package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 16.10.15.
 */
@Stateless
@LocalBean
public class GLAccountRequestRepository extends AbstractBaseEntityRepository<GLAccountRequest, String> {

    /**
     * Проверяет наличие в БД счета Майдас
     *
     * @param Acid - счет Майдас
     * @param Bsaacid - счет цб
     * @return - long - id
     * этот код дублируется в тригере GL_ACC_BI befor (если NROW.id будет уже заполнен(в данном случае), тригер ничего не делает)
     * в тригере GL_ACC after создается запись в accrlnext, если там нет такого glacid
     * в accrlnext уникальность по bsaacid
     * в accrln уникальность по bsaacid, acid
     */
//    public Long getGlAccId(String Acid, String Bsaacid){
//        Long id;
//        try{
//            //max - чтобы получить null, если нет записи
//            DataRecord res = selectFirst("select max(x.glacid) from accrlnext x where x.acid = ? and x.bsaacid = ?" , Acid, Bsaacid);
//            id = res.getLong(0);
//            if (id == null) {
//                id = nextId("GL_SEQ_ACC");
//                executeNativeUpdate("insert into accrlnext (GLACID, ACID, BSAACID, INP_MTHD) values(?,?,?,'1')"
//                        , id, ifEmpty(Acid, " "), Bsaacid);
//            }
//        } catch (SQLException e) {
//            throw new DefaultApplicationException(e.getMessage(), e);
//        }
//        return id;
//    }

    /**
     * Загрузка запросов на открытие счета
     * @param maxRows строк к обработке
     * @param hoursOld старше чем systimestam - hoursOld не обрабатываем
     * @return
     */
    public List<DataRecord> getRequestForProcessing(int maxRows, int hoursOld) {
        try {
            return selectMaxRows("SELECT * FROM GL_ACOPENRQ WHERE STATUS = 'NEW' AND SYS_TIME > SYSTIMESTAMP - ?/24 ORDER BY SYS_TIME", maxRows, new Object[]{hoursOld});
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Обновляет статус обработанного запроса
     * @param request
     * @param status
     */
    public void updateRequestStateProcessed(GLAccountRequest request, GLAccountRequest.RequestStatus status) {
        executeUpdate("update GLAccountRequest r set r.status = ?1 where r.id = ?2"
                , status, request.getId());
    }

    /**
     * Создает запись с ответом на запрос создания счета
     * @param request   - запрос на создание счета
     * @param newAcc    -
     * @param error
     * @param descr
     */
    public void createResponse(GLAccountRequest request, GLAccount glAccount, boolean newAcc, String error, String descr) {
        String cbAccount = null;
        String acod = null;
        String seq = null;
        Date dateOpen = null;   // TODO request.getDateOpen() ?
        if (null != glAccount) {
            cbAccount = glAccount.getBsaAcid();
            acod = format("%4d", glAccount.getAccountCode());
            seq = format("%2d", glAccount.getAccountSequence());
            dateOpen = glAccount.getDateOpen();
        }
        String sql = "insert into GL_ACOPENRS (" +
                    "REQUEST_ID, CBACCOUNT_NO, NEWACC, OPEN_DATE, ACOD, SEQ, ERRORCODE, ERRORDESCRIPTION" +
                    ") values (?, ?, ?, ?, ?, ?, ?, ?)";
        executeNativeUpdate(sql, request.getId(), cbAccount, newAcc ? "Y" : "N", dateOpen, acod, seq,
                substr(error, 4), substr(descr, 1024));
    }

    /**
     * Создает запись о событии создания счета
     * @param request   - запрос на создание счета
     */
    public void createEvent(GLAccountRequest request) {
        // TODO предполагаем, что EVENT_ID - автоинкремент, EVENT_TIME - default,
        // TODO , EVENT_COMMENT - заполняет адаптер
        String sql = "insert into WBI_EVENTS (" +
                "EVENT_ID, CONNECTOR_ID, OBJECT_KEY, OBJECT_NAME, OBJECT_VERB, EVENT_PRIORITY, EVENT_STATUS" +
                ") values (WBI_EVENTS_SEQ.nextval, 'UcbruBarsGLAdapter', ?, 'ABARSGLAccountRetrieveResponse', 'Retrieve', 0, 0)";
        executeNativeUpdate(sql, request.getId());
    }
}
