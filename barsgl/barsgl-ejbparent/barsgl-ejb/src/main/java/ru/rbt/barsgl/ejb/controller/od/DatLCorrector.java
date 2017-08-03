package ru.rbt.barsgl.ejb.controller.od;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

/**
 * Created by er21006 on 01.08.2017.
 * корректирует поле BALTUR.DATL по дате последней операции
 */
@Stateless
@LocalBean
public class DatLCorrector {

    @EJB
    private CoreRepository repository;

    @Inject
    private TextResourceController textResourceController;

    @EJB
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    /**
     * корректируем baltur на основании журнала GL_PDJCHG
     * @return кол-во
     * @throws Exception
     */
    public long correctDatL() throws Exception {
        List<DataRecord> datesFrom = repository.select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/datl_acc_list.sql")
                , operdayController.getOperday().getCurrentDate());
        final int[] cntUpdated = {0};
        datesFrom.forEach(d -> {
            try {
                List<BalturDTL> balturs = convert(repository.select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/datl_baltur_acc.sql")
                        , d.getString("ACID"), d.getString("BSAACID"), dateUtils.addDays(d.getDate("MIN_POD"), -1), d.getDate("MIN_POD")));
                balturs.forEach(b -> {
                    if (b.getDat().compareTo(d.getDate("min_pod")) >= 0) {
                        if ((abs(b.getDtac()) + abs(b.getCtac())) > 0) {
                            if (b.getDat().compareTo(b.getDatL()) != 0) {
                                updateBaltur(b.getAcid(), b.getBsaacid(), b.getDat(), b.getDat(), b);
                                cntUpdated[0]++;
                            }
                        } else {
                            BalturDTL prev = findPrevious(balturs, b);
                            updateBaltur(b.getAcid(), b.getBsaacid(), b.getDat(), prev != null ? prev.getDatL() : null, b);
                            cntUpdated[0]++;
                        }
                    }
                });
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        });
        return cntUpdated[0];
    }

    private List<BalturDTL> convert(List<DataRecord> fromBalturs) {
        return fromBalturs.stream().map(r ->
                new BalturDTL(r.getString("acid"), r.getString("bsaacid"), r.getDate("dat"), r.getDate("datl")
                        , r.getLong("dtac"), r.getLong("ctac"))).collect(Collectors.toList());
    }

    private void updateBaltur(String acid, String bsaacid, Date dat, Date datL, BalturDTL baltur) {
        baltur.setDatL(datL);
        Assert.isTrue(1 == repository.executeNativeUpdate("update baltur set datl = ? where acid = ? and bsaacid = ? and dat = ?"
                ,datL, acid, bsaacid, dat), () -> new DefaultApplicationException(String.format("Baltur row is not exists for acid %s, bsaacid %s, dat %s", acid, bsaacid, dateUtils.onlyDateString(dat))));
    }

    private BalturDTL findPrevious(List<BalturDTL> balturs, BalturDTL relatively) {
        int index = balturs.indexOf(relatively) - 1;
        return index >= 0 ? balturs.get(index) : null;
    }


    private class BalturDTL {
        private final String acid;
        private final String bsaacid;
        private final Date dat;
        private final Long dtac;
        private final Long ctac;
        private Date datL;

        public BalturDTL(String acid, String bsaacid, Date dat, Date datL, long dtac, long ctac) {
            this.acid = acid;
            this.bsaacid = bsaacid;
            this.dat = dat;
            this.datL = datL;
            this.dtac = dtac;
            this.ctac = ctac;
        }

        public String getAcid() {
            return acid;
        }

        public String getBsaacid() {
            return bsaacid;
        }

        public Date getDat() {
            return dat;
        }

        public Date getDatL() {
            return datL;
        }

        public Long getDtac() {
            return dtac;
        }

        public Long getCtac() {
            return ctac;
        }

        public void setDatL(Date datL) {
            this.datL = datL;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BalturDTL balturDTL = (BalturDTL) o;

            if (!acid.equals(balturDTL.acid)) return false;
            if (bsaacid != null ? !bsaacid.equals(balturDTL.bsaacid) : balturDTL.bsaacid != null) return false;
            return dat != null ? dat.equals(balturDTL.dat) : balturDTL.dat == null;
        }

        @Override
        public int hashCode() {
            int result = acid.hashCode();
            result = 31 * result + (bsaacid != null ? bsaacid.hashCode() : 0);
            result = 31 * result + (dat != null ? dat.hashCode() : 0);
            return result;
        }
    }
}
