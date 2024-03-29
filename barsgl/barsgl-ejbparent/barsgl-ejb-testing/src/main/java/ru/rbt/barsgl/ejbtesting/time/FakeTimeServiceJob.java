package ru.rbt.barsgl.ejbtesting.time;


import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import java.util.Date;
import ru.rbt.barsgl.ejb.common.controller.od.SystemTimeService;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
public class FakeTimeServiceJob implements SystemTimeService {

    private Date currentDate;

    @Override
    public Date getCurrentTime() {
        return currentDate;
    }

    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    @PostConstruct
    public void init() {
        currentDate = new Date();
    }
}
