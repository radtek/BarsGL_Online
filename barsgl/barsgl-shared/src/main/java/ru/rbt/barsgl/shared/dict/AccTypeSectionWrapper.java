package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeSectionWrapper implements Serializable,IsSerializable {
    private String section;
    private String sectionName;

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String name) {
        sectionName = name;
    }
}
