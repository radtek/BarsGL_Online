package ru.rbt.barsgl.ejb.repository.customer;

import ru.rbt.barsgl.ejb.entity.cust.CustDNInput;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.Map;

/**
 * Created by er18837 on 15.12.2017.
 */
public class CustDNInputRepository extends AbstractBaseEntityRepository<CustDNInput, Long> {

    public CustDNInput createInputParams(Long id, Map<String, String> xmlData) {
        CustDNInput journal = new CustDNInput();
        journal.setId(id);
        journal.setCustNo(xmlData.get("CUST_NUM"));
        journal.setBranch(xmlData.get("BRANCHCODE"));
        journal.setFcCbType(xmlData.get("CBTYPE"));
        journal.setFcCustType(xmlData.get("FCTYPE"));
        journal.setResident(xmlData.get("RESIDENT"));
        journal.setNameEng(xmlData.get("NAME_ENG"));
        journal.setNameRus(xmlData.get("NAME_RUS"));
        journal.setLegalForm(xmlData.get("LEGAL_FORM"));
        journal = save(journal);
        return journal;
    }


}
