package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.entity.dict.dwh.Branchs;
import ru.rbt.barsgl.ejb.entity.dict.dwh.BranchsInf;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;

import javax.ejb.EJB;
import java.util.Date;

/**
 * Created by er22317 on 14.02.2018.
 */
public class LoadDictBr  extends LoadDict<BranchsInf, Branchs> {
    @EJB
    private BranchDictRepository branchDictRepository;
    public LoadDictBr() {super(BranchsInf.class, Branchs.class);};

    @Override
    protected String sqlVitrina() {
        return "select A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM from V_GL_DWH_IMBCBBRP";
    }

    @Override
    protected boolean getFixFilter(BranchsInf item, Date dateLoad) {
        return item.getId().compareTo(getFixBranchCode()) > 0 && item.getValidFrom().compareTo(dateLoad) >= 0;
    }

    @Override
    protected boolean getIdFilter(BranchsInf itemInf, Branchs item) {
        return itemInf.getId().equals(item.getId());
    }

    @Override
    protected void saveT(BranchsInf item) {
        branchDictRepository.saveEntityNoFlash(new Branchs(item.getId(), item.getA8LCCD(), item.getA8LCCD(), item.getA8BICN(), item.getA8BRNM(), item.getBBRRI(), item.getBCORI(), item.getBCBBR(), item.getBR_HEAD(), item.getBR_OPER()));
    }

    @Override
    protected String insMap(BranchsInf item) {
        branchDictRepository.nativeUpdate("insert into DH_BR_MAP(FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH) values (?,?,?)",
                                            new Object[]{item.getFCC_CODE(), item.getId(), item.getBCBBR()});
        return item.getId();

    }

    @Override
    protected String updE(BranchsInf item, Branchs f) {
        if (!item.getA8LCCD().equals(f.getA8CMCD()) ||
            !item.getA8LCCD().equals(f.getA8LCCD()) ||
            !item.getA8BICN().equals(f.getA8BICN()) ||
            !item.getA8BRNM().equals(f.getA8BRNM()) ||
            !item.getBBRRI().equals(f.getBBRRI()) ||
            !item.getBCORI().equals(f.getBCORI()) ||
            !item.getBCBBR().equals(f.getBCBBR()) ||
            !item.getBR_HEAD().equals(f.getBR_HEAD()) ||
            !item.getBR_OPER().equals(f.getBR_OPER()) ) {
            Branchs branchsUpd = (Branchs) branchDictRepository.findById(Branchs.class, item.getId());
            branchsUpd.setA8CMCD(item.getA8LCCD());
            branchsUpd.setA8LCCD(item.getA8LCCD());
            branchsUpd.setA8BICN(item.getA8BICN());
            branchsUpd.setA8BRNM(item.getA8BRNM());
            branchsUpd.setBBRRI(item.getBBRRI());
            branchsUpd.setBCORI(item.getBCORI());
            branchsUpd.setBCBBR(item.getBCBBR());
            branchsUpd.setBR_HEAD(item.getBR_HEAD());
            branchsUpd.setBR_OPER(item.getBR_OPER());
            branchDictRepository.jpaUpdateNoFlash(branchsUpd);
            branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
                    new Object[]{item.getId(), item.getBCBBR(), item.getFCC_CODE()});
            return item.getId()+ " ";
        }
        return "";
    }
}
