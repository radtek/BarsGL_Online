package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;
import static ru.rbt.barsgl.shared.Repository.BARSGLNOXA;


/**
 * Created by er22317 on 14.02.2018.
 */
public abstract class LoadDict<E, F> {
    private Class<E> clazzE;
    private Class<F> clazzF;

    public LoadDict(Class<E> clazzE, Class<F> clazzF) {
        this.clazzE = clazzE;
        this.clazzF = clazzF;
    }

    @EJB
    private PropertiesRepository propertiesRepository;
    @EJB
    private BranchDictRepository branchDictRepository;
    @EJB
    private AuditController auditController;
//E - filialsInf, F - Filials
    public void fillTargetTables(Date dateLoad, long _loadStatId) throws Exception {
        StringBuilder insFils = new StringBuilder();
        StringBuilder updFils = new StringBuilder();

        List<E> listInf = branchDictRepository.tableToList(clazzE, sqlVitrina());
        branchDictRepository.listToTable(listInf);

        List<E> fixList = new ArrayList<E>();
        listInf.stream().filter(item->getFixFilter(item, dateLoad)).forEach(item -> fixList.add(item));

        List<F> target = branchDictRepository.getAll(clazzF);
        fillTransient(branchDictRepository.getMapAll(), target);

        auditController.info(LoadBranchDict, "LoadBranchDictTask витрина "+clazzF.getSimpleName()+" загружена из dwh (" + listInf.size()+" записей)", "", String.valueOf(_loadStatId));
        Collections.sort((ArrayList)target);
        for (E item : fixList) {
            Optional<F> f = target.stream().filter(x ->getIdFilter(item, x)).findFirst();
            if (!f.isPresent()) {
                //добавить в целевые
                saveT(item);
                insFils.append(insMap(item)).append(" ");
            }
            //обновить целевые
            else {
                updFils.append(updE(item, f.get()));
            }
        }
        branchDictRepository.flush(BARSGLNOXA);
        if (insFils.length() > 0 || updFils.length() > 0)
            auditController.info(LoadBranchDict, "LoadBranchDictTask "+clazzF.getSimpleName()+ " " +(insFils.length()>0?"добавлены с кодом "+insFils:"")+ (updFils.length()>0?"обновлены с кодом "+updFils:""), "", String.valueOf(_loadStatId));
        else
            auditController.info(LoadBranchDict,"LoadBranchDictTask справочник "+clazzF.getSimpleName()+" не обновлен - нет данных для обновления", "", String.valueOf(_loadStatId));
    }


    public String getFixBranchCode() {
        try {
            StringBuilder s = new StringBuilder("000").append(String.valueOf(propertiesRepository.getNumber("dwh.fix.branch.code").intValue()));
            return s.substring(s.length() - 3);
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    protected abstract String sqlVitrina();
    protected abstract boolean getFixFilter(E item, Date dateLoad);
    protected abstract boolean getIdFilter( E itemInf, F item);
    protected abstract void saveT(E item) throws Exception;
    protected abstract String insMap(E item) throws SQLException;
    protected abstract String updE(E item, F f) throws Exception;
    protected abstract void fillTransient(List<DataRecord> map, List<F> target);
}
