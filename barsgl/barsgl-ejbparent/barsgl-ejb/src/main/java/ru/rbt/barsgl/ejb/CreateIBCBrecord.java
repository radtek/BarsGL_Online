package ru.rbt.barsgl.ejb;

import ru.rbt.barsgl.bankjar.CBAccount;
import ru.rbt.barsgl.bankjar.Constants;
import ru.rbt.barsgl.bankjar.RequestContextHolder;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Created by er22317 on 05.03.2018.
 */
public class CreateIBCBrecord {
    @EJB
    private GLAccountRepository glAccountRepository;
    @EJB
    private GLAccountController glAccountController;
    @EJB
    private OperdayController operdayController;

    private Date currDate;

    public void calculateIBCBaccount(String brcaFrom, String brcaTo, String ccy)
            throws SQLException {

        // обязательна инициализация контекста при работе с компонентами "BANKJAR"
//        context.setCurrDay(new Date(operdayController.getOperday().getCurrentDate().getTime()));

        String cbccy = glAccountRepository.getCbccy(ccy);
        StringBuilder fromCode = new StringBuilder();
        StringBuilder fromBic = new StringBuilder();
        StringBuilder toCode = new StringBuilder();
        StringBuilder toBic = new StringBuilder();
        glAccountRepository.getBicControlCode(brcaFrom, fromCode, fromBic);
        glAccountRepository.getBicControlCode(brcaTo, toCode, toBic);
        String lcvFrom = "", lcvFrom5 = "";
        String lcvTo = "", lcvTo5 = "";
        //Для Мросквы и Питера изначально перепутаны номера в хвосте счета.
        //Для новых счетов 30305/6 можно делать по обшей схеме.
        if (brcaFrom.equals("MOS") && brcaTo.equals("SPB")) {
            lcvFrom = "0000001";
            lcvTo = "0000002";
            lcvFrom5 = "0000002";
            lcvTo5 = "0000001";
        } else if (brcaFrom.equals("SPB") && brcaTo.equals("MOS")) {
            lcvFrom = "0000002";
            lcvTo = "0000001";
            lcvFrom5 = "0000001";
            lcvTo5 = "0000002";
        } else {
            String space = "0000000";
            lcvFrom = String.valueOf(space.substring(0, 7 - toCode.length())) + toCode.toString();
            lcvTo = String.valueOf(space.substring(0, 7 - fromCode.length())) + fromCode.toString();
            lcvFrom5 = lcvFrom;
            lcvTo5 = lcvTo;
        }
        try {
            calculateIBCBaccount(ccy, cbccy, fromBic.toString(), fromCode.toString(), lcvFrom, brcaFrom, brcaTo, lcvFrom5);
            calculateIBCBaccount(ccy, cbccy, toBic.toString(), toCode.toString(), lcvTo, brcaTo, brcaFrom, lcvTo5);
        } catch (Exception e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void calculateIBCBaccount(String glccy, String cbccy, String bicCode, String brca1,
                                      String lclCode, String brcaFrom, String brcaTo, String lclCode5) throws Exception {
        currDate = operdayController.getOperday().getCurrentDate();
        String cb1 = calculateControlNumber(bicCode, "30301"+cbccy+"0"+brca1+lclCode);
//        /*process.*/logger.debug("Calculate account =" + cb1);
//        if(!process.existBsaAccount(cb1))
        if(!glAccountRepository.checkAccountExists(cb1))
            glAccountController.createGLAccountMF( cb1, Constants.PASIV, currDate, glccy);

        String cb2 = calculateControlNumber(bicCode, "30302"+cbccy+"0"+brca1+lclCode);
//        /*process.*/logger.debug("Calculate account ="+cb2);
//        if(!process.existBsaAccount(cb2))
        if(!glAccountRepository.checkAccountExists(cb2))
            glAccountController.createGLAccountMF( cb2, Constants.ACTIV, currDate, glccy);

        String cb305 = calculateControlNumber(bicCode, "30305"+cbccy+"0"+brca1+lclCode5);
//        /*process.*/logger.debug("Calculate account ="+cb305);
//        if(!process.existBsaAccount( cb305))
        if(!glAccountRepository.checkAccountExists(cb305))
            glAccountController.createGLAccountMF( cb305, Constants.PASIV, currDate, glccy);

        String cb306 = calculateControlNumber(bicCode, "30306"+cbccy+"0"+brca1+lclCode5);
//        /*process.*/logger.debug("Calculate account ="+cb306);
//        if(!process.existBsaAccount( cb306))
        if(!glAccountRepository.checkAccountExists(cb306))
            glAccountController.createGLAccountMF( cb306, Constants.ACTIV, currDate, glccy);

        if(!glAccountRepository.existIbcb(cb1, cb2, glccy ))
            glAccountController.insertIbcb(brcaFrom, brcaTo, glccy, cb1, cb2, cb305, cb306);
    }

    final String code6 = new String("ABCEHKMPTX");
    static int coeficient[] = {
            7, 1, 3
    };

    public String calculateControlNumber(String bic, String account)
    {
        String sCode = String.valueOf(bic) + String.valueOf(account);
        int k = 0;
        for(int i = 0; i < sCode.length(); i++)
        {
            if(i == 11)
                continue;
            String simbol = sCode.substring(i, i + 1);
            int iSimbol = 0;
            try
            {
                iSimbol = Integer.valueOf(simbol).intValue();
            }
            catch(NumberFormatException ex)
            {
                iSimbol = code6.indexOf(simbol);
            }
            k += (coeficient[i % 3] * iSimbol) % 10;
        }

        int controlNumber = ((k % 10) * 3) % 10;
        return String.valueOf(String.valueOf((new StringBuilder(String.valueOf(String.valueOf(account.substring(0, 8))))).append(String.valueOf(controlNumber)).append(account.substring(9))));
    }

}
