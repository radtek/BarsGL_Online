package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.user.client.ui.Grid;

/**
 * Created by akichigi on 10.03.17.
 */
public class GradientProgressBar extends ProgressBar {
    private boolean showGradientColor = false;
    private String gradientStopStyle1 = "progressbar-0";
    private String gradientStopStyle2 = "progressbar-33";
    private String gradientStopStyle3 = "progressbar-66";

    public GradientProgressBar(int elements, int options) {
        super(elements, options);
    }

    public GradientProgressBar(int elements) {
        super(elements);
    }

    public GradientProgressBar(int elements, int options, boolean isBarSmooth) {
        super(elements, options, isBarSmooth);
    }

    public boolean isShowGradientColor() {
        return showGradientColor;
    }

    public void setShowGradientColor(boolean showGradientColor) {
        this.showGradientColor = showGradientColor;
    }

    @Override
    protected void competeProgressStyle(Grid elm, int offset){
        if (showGradientColor) elm.setStyleName(getGradientColorStyle(offset * 100 / elements));
        else
        elm.setStyleName("progressbar-fullbar");
        elm.addStyleName(elmStyle);
    }

    private String getGradientColorStyle(int offset){
        if (offset >= 0 && offset < 33) return  gradientStopStyle1;
        if (offset >= 33 && offset < 66) return gradientStopStyle2;
        if (offset >= 66 && offset <= 100) return gradientStopStyle3;
        return "progressbar-fullbar";
    }

    public void setGradientStopStyle1(String gradientStopStyle1) {
        this.gradientStopStyle1 = gradientStopStyle1;
    }

    public void setGradientStopStyle2(String gradientStopStyle2) {
        this.gradientStopStyle2 = gradientStopStyle2;
    }

    public void setGradientStopStyle3(String gradientStopStyle3) {
        this.gradientStopStyle3 = gradientStopStyle3;
    }
}
