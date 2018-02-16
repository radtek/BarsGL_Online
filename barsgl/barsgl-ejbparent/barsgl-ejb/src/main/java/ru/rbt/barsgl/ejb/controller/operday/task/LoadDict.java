package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.dict.dwh.Filials;
import ru.rbt.barsgl.ejb.entity.dict.dwh.FilialsInf;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.ejb.EJB;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;

/**
 * Created by er22317 on 14.02.2018.
 */
public abstract class LoadDict<E, F> {
    public static final String streamId = "DWH_BRANCH_LOAD";
    public static final String propOperDay = "operday";
    long _loadStatId;
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
    public void fillTargetTables(Date dateLoad ) {
        StringBuilder insFils = new StringBuilder();
        StringBuilder updFils = new StringBuilder();

        List<E> listInf = branchDictRepository.tableToList(clazzE, sqlVitrina());
        branchDictRepository.listToTable(listInf);
        auditController.info(LoadBranchDict, "LoadBranchDictTask промежуточные тавлицы "+clazzF.getSimpleName()+" заполнены", "", String.valueOf(_loadStatId));

        List<E> fixList = new ArrayList<E>();
        listInf.stream().filter(item->getFixFilter(item, dateLoad)).forEach(item -> fixList.add(item));

        List<F> target = branchDictRepository.getAll(clazzF);
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
        branchDictRepository.flush();
        auditController.info(LoadBranchDict, "LoadBranchDictTask "+clazzF.getSimpleName()+ " " +(insFils.length()>0?"добавлены "+insFils:"")+ (updFils.length()>0?"обновлены "+updFils:""), "", String.valueOf(_loadStatId));
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
    protected abstract void saveT(E item);
    protected abstract String insMap(E item);
    protected abstract String updE(E item, F f);

}
