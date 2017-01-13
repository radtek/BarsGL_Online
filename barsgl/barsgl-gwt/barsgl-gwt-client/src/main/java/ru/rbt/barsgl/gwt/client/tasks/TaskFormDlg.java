package ru.rbt.barsgl.gwt.client.tasks;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.barsgl.shared.enums.JobStartupType;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;

/**
 * Created by akichigi on 19.03.15.
 */
public class TaskFormDlg extends DlgFrame{

    private TimerJobWrapper timerJobs;
    private ListBox vRunType;
    private TextArea vProps;
    private TxtBox vJobType;
    private TxtBox vShedule;
    private Label lShedule;

    public TaskFormDlg()
    {
        super();
        setCaption("Править");
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(4, 2);

        grid.setWidget(0, 0, createLabel("Тип запуска"));
        grid.setWidget(0, 1, createRunTypes());
        grid.setWidget(1, 0, createLabel("Свойства"));
        grid.setWidget(1, 1, createProperty());
        grid.setWidget(2, 0, createLabel("Тип задания"));
        grid.setWidget(2, 1, vJobType = createTxtBox(64, "300px"));
        vJobType.setEnabled(false);
        grid.setWidget(3, 0, lShedule = createLabel("Расписание"));
        grid.setWidget(3, 1, vShedule = createTxtBox(512, "300px"));

        return grid;
    }

    @Override
      public boolean onClickOK(){
        timerJobs.setProperties(vProps.getText());
        timerJobs.setStartupType(getEnumValue(vRunType.getItemText(vRunType.getSelectedIndex())));
        timerJobs.setScheduleExpression(vShedule.getValue());
        timerJobs.setJobType(null);     // на всякий случай, чтобы не проскочило на сервер

        params = timerJobs;
        return true;
    }

    @Override
    protected void fillContent() {
        timerJobs = (TimerJobWrapper)params;

        vProps.setText(timerJobs.getProperties());
        vRunType.setItemSelected(getEnumIndex(timerJobs.getStartupType().getLabel()), true);
        vJobType.setValue(timerJobs.getJobType().getLabel());
        boolean isCalendar = timerJobs.getJobType().equals(JobSchedulingType.CALENDAR);
        vShedule.setValue(timerJobs.getScheduleExpression());
        vShedule.setVisible(isCalendar);
        lShedule.setVisible(isCalendar);
    }

    private ListBox createRunTypes(){
        vRunType = new ListBox(false);
        vRunType.setWidth("100%");

        for( JobStartupType x: JobStartupType.values() ){
            vRunType.addItem(x.getLabel());
        }

        return vRunType;
    }

    private TextArea createProperty() {
        vProps = new TextArea();
        vProps.setSize("300px", "50px");

        return vProps;
    }

    private JobStartupType getEnumValue(String label){
        for (JobStartupType x: JobStartupType.values()){
            if( x.getLabel().equalsIgnoreCase(label))
                return x;
        }
        return null;
    }

    private int getEnumIndex(String label){
        for (int i=0;  i < vRunType.getItemCount(); i++){
            if (vRunType.getItemText(i).equalsIgnoreCase(label)){
                return i;
            }
        }
        return -1;
    }
}