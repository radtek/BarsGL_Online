package ru.rbt.barsgl.bankjar;

import ru.rbt.barsgl.ejbcore.CoreRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class IDGenerator {

    @EJB
    private CoreRepository repository;

    public long nextId() throws SQLException {
        return repository.selectFirst("select (next value for pd_seq) id_seq from sysibm.sysdummy1").getLong("ID_SEQ");
    }

}
