package ru.rbt.barsgl.shared.jobs;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov on 01.11.2016.
 * трансфер объект истории запуска задачи
 */
public class TimerJobHistoryWrapper implements Serializable{

    private static final long serialVersionUID = -1L;

    private Long idHistory;

    public TimerJobHistoryWrapper(Long idHistory) {
        this.idHistory = idHistory;
    }

    public TimerJobHistoryWrapper() {
    }

    public Long getIdHistory() {
        return idHistory;
    }

    public void setIdHistory(Long idHistory) {
        this.idHistory = idHistory;
    }
}
