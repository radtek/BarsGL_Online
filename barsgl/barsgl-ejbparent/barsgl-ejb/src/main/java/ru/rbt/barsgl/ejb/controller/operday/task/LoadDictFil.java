package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.dict.dwh.Filials;
import ru.rbt.barsgl.ejb.entity.dict.dwh.FilialsInf;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.rbt.barsgl.shared.Repository.BARSGLNOXA;

/**
 * Created by er22317 on 14.02.2018.
 */
public class LoadDictFil extends LoadDict<FilialsInf, Filials> {
    @EJB
    private BranchDictRepository branchDictRepository;

    public LoadDictFil() {
        super(FilialsInf.class, Filials.class);
    }

    @Override
    public String sqlVitrina() {
        return "select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP";
    }

    @Override
    protected boolean getFixFilter(FilialsInf item, Date dateLoad) {
        return item.getAltCode().compareTo(getFixBranchCode()) > 0 && item.getValidFrom().compareTo(dateLoad) >= 0;
    }

    @Override
    protected boolean getIdFilter(FilialsInf itemInf, Filials item) {
        return itemInf.getId().equals(item.getId());
    }

    @Override
    protected void saveT(FilialsInf item) throws Exception {
        branchDictRepository.saveEntityNoFlash(new Filials(item.getId(), item.getCcpne(), item.getCcpnr(), item.getCcpri(), item.getCcbbr()));

    }

    @Override
    protected String insMap(FilialsInf item) throws SQLException{
        if (null == branchDictRepository.selectFirst( "select FCC_BRANCH from DH_BR_MAP where FCC_BRANCH=?", item.getId())) {
            branchDictRepository.nativeUpdate("insert into DH_BR_MAP(FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH) values (?,?,?)",
                    new Object[]{item.getId(), item.getAltCode(), item.getCcbbr()});
        }else{
//            updateMap(item);
            branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
                    new Object[]{item.getAltCode(), item.getCcbbr(), item.getId()});

        }
        return item.getId();
    }

    @Override
    protected String updE(FilialsInf item, Filials f) throws Exception {
        if (!item.getCcpne().equals(f.getCcpne()) ||
                !item.getCcpnr().equals(f.getCcpnr()) ||
                !item.getCcpri().equals(f.getCcpri()) ||
                !item.getCcbbr().equals(f.getCcbbr()) ||
                !item.getAltCode().equals(f.getALT_CODE())) {
            Filials filialsUpd = (Filials) branchDictRepository.findByIdNoXa(Filials.class, item.getId());
            filialsUpd.setCcpne(item.getCcpne());
            filialsUpd.setCcpnr(item.getCcpnr());
            filialsUpd.setCcpri(item.getCcpri());
            filialsUpd.setCcbbr(item.getCcbbr());
            branchDictRepository.jpaUpdateNoFlash(filialsUpd);
            insMap(item);
//            branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
//                    new Object[]{item.getAltCode(), item.getCcbbr(), item.getId()});
            return item.getId() + " ";
        }
        return "";
    }

    @Override
    protected void fillTransient(List<DataRecord> map, List<Filials> target){
        for(Filials targetItem: target){
            for(DataRecord mapItem: map){
                if (targetItem.getId().equals(mapItem.getString("FCC_BRANCH"))){
                    targetItem.setALT_CODE(mapItem.getString("MIDAS_BRANCH"));
                    continue;
                }
            }
        }
    }

//    void updateMap(FilialsInf item) {
//        branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
//                new Object[]{item.getAltCode(), item.getCcbbr(), item.getId()});
//    }
}