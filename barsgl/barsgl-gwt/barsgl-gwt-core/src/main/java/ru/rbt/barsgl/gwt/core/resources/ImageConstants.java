package ru.rbt.barsgl.gwt.core.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public abstract interface ImageConstants extends ClientBundle{

    public static final ImageConstants INSTANCE = (ImageConstants)GWT.create(ImageConstants.class);

    @ClientBundle.Source({"refresh24.png"})
    public abstract ImageResource refresh24();

    @ClientBundle.Source({"refresh16.gif"})
    public abstract ImageResource refresh16();

    @ClientBundle.Source({"reload16.png"})
    public abstract ImageResource reload16();

    @ClientBundle.Source({"edit24.png"})
    public abstract ImageResource edit24();

    @ClientBundle.Source({"run.png"})
    public abstract ImageResource run();

    @ClientBundle.Source({"run_all.png"})
    public abstract ImageResource run_all();

    @ClientBundle.Source({"run_once.png"})
    public abstract ImageResource run_once();

    @ClientBundle.Source({"stop.png"})
    public abstract ImageResource stop();

    @ClientBundle.Source({"stop_all.png"})
    public abstract ImageResource stop_all();

    @ClientBundle.Source({"plus16.png"})
    public abstract ImageResource plus16();

    @ClientBundle.Source({"minus16.png"})
    public abstract ImageResource minus16();

    @ClientBundle.Source({"filter.png"})
    public abstract ImageResource filter();

    @ClientBundle.Source({"filterOn.png"})
    public abstract ImageResource filterOn();

    @ClientBundle.Source({"quickfilter.png"})
    public abstract ImageResource quickfilter();

    @ClientBundle.Source({"about16.png"})
    public abstract ImageResource about16();

    @ClientBundle.Source({"calendar.png"})
    public abstract ImageResource calendar();

    @ClientBundle.Source({"properties.png"})
    public abstract ImageResource properties();

    @ClientBundle.Source({"new24.png"})
    public abstract ImageResource new24();

    @ClientBundle.Source({"more16.png"})
    public abstract ImageResource more16();

    @ClientBundle.Source({"excel.png"})
    public abstract ImageResource excel();

    @ClientBundle.Source({"oper_go.png"})
    public abstract ImageResource oper_go();

    @ClientBundle.Source({"close24.png"})
    public abstract ImageResource close24();

    @ClientBundle.Source({"oper_tmpl.png"})
    public abstract ImageResource oper_tmpl();

    @ClientBundle.Source({"back_value.png"})
    public abstract ImageResource back_value();

    @ClientBundle.Source({"clean_cache.png"})
    public abstract ImageResource clean_cache();

    @ClientBundle.Source({"backward.png"})
    public abstract ImageResource backward();

    @ClientBundle.Source({"forward.png"})
    public abstract ImageResource forward();

    @ClientBundle.Source({"page_key.png"})
    public abstract ImageResource page_key();

    @ClientBundle.Source({"function.png"})
    public abstract ImageResource function();

    @ClientBundle.Source({"site_map.png"})
    public abstract ImageResource site_map();

    @ClientBundle.Source({"increase.png"})
    public abstract ImageResource increase();

    @ClientBundle.Source({"decrease.png"})
    public abstract ImageResource decrease();

    @ClientBundle.Source({"date_edit.png"})
    public abstract ImageResource date_edit();

    @ClientBundle.Source({"sign.png"})
    public abstract ImageResource sign();

    @ClientBundle.Source({"load.png"})
    public abstract ImageResource load();

    @ClientBundle.Source({"load_blue.png"})
    public abstract ImageResource load_blue();

    @ClientBundle.Source({"load_acc.png"})
    public abstract ImageResource load_acc();

    @ClientBundle.Source({"preview.png"})
    public abstract ImageResource preview();

    @ClientBundle.Source({"statistics.png"})
    public abstract ImageResource statistics();

    @ClientBundle.Source({"fromdict16.png"})
    public abstract ImageResource fromdict16();

    @ClientBundle.Source({"new16.png"})
    public abstract ImageResource new16();

    @ClientBundle.Source({"dice.png"})
    public abstract ImageResource dice();

    @ClientBundle.Source({"refresh_tab.png"})
    public abstract ImageResource refresh_tab();

    @ClientBundle.Source({"refresh_simple.png"})
    public abstract ImageResource refresh_simple();

    @ClientBundle.Source({"coins.png"})
    public abstract ImageResource coins();

    @ClientBundle.Source({"process.png"})
    public abstract ImageResource process();

    @ClientBundle.Source({"ok.png"})
    public abstract ImageResource ok();

    @ClientBundle.Source({"display.png"})
    public abstract ImageResource display();

    @ClientBundle.Source({"report.png"})
    public abstract ImageResource report();

    @ClientBundle.Source({"copy.png"})
    public abstract ImageResource copy();

    @ClientBundle.Source({"authoriz.png"})
    public abstract ImageResource authoriz();

    @ClientBundle.Source({"locked.png"})
    public abstract ImageResource locked();

    @ClientBundle.Source({"nonauthoriz.png"})
    public abstract ImageResource nonauthoriz();

    @ClientBundle.Source({"link.png"})
    public abstract ImageResource link();

    @ClientBundle.Source({"link_break.png"})
    public abstract ImageResource link_break();

    @ClientBundle.Source({"male.png"})
    public abstract ImageResource male();

}
