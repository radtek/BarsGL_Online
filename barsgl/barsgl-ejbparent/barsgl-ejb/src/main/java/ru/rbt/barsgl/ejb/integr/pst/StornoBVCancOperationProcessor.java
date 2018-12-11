package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_REF_NOT_VALID;

/**
 * Created by er18837 on 26.11.2018.
 */
public class StornoBVCancOperationProcessor extends GLOperationProcessor{

    @Inject
    private GLOperationRepository glOperationRepository;

    @EJB
    private BackvalueJournalRepository backvalueRepository;

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
    }

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoBVCanc(operation)                                                            // сторно в тот же день
                && DIRECT == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         // признак операции по техническим счетам
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        operation.setStornoRegistration(GLOperation.StornoType.C);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        GLOperation stornoOperation = operation.getStornoOperation();
        OperState operState = stornoOperation.getState();

        // Операция должна быть обработана
        if ( operState.equals(OperState.POST) ) {
            List<GLPosting> stornoList = glOperationRepository.getPostings(stornoOperation);
            if (null == stornoList || stornoList.isEmpty()) {
                throw new ValidationError(STORNO_REF_NOT_VALID,
                        stornoOperation.getId().toString(), operState.name() + " (нет проводок)", OperState.POST.name());
            }
            pdRepository.updatePdInvisible(true, stornoList);
            // запись в журнал BV
            List<Pd> pdList = pdRepository.getPdListByPostings(stornoList);
            for (Pd pd : pdList) {
                backvalueRepository.registerBackvalueJournalAcc(pd.getBsaAcid(), pd.getAcid(), pd.getPod());
            }
        }
        else {  // Операция не обработана - ошибка
            throw new ValidationError(STORNO_REF_NOT_VALID,
                    stornoOperation.getId().toString(), operState.name(), OperState.POST.name() + " или " + OperState.WTAC.name());
        }

        // статус сторнируемой операции - CANC
        glOperationRepository.updateOperationStatus(stornoOperation, OperState.CANC);

        // TODO AN_IND=’1’ ??
        return Collections.emptyList();
    }

    @Override
    public OperState getSuccessStatus() {
        return OperState.SOCANC;
    }
}
