package ru.rbt.barsgl.shared.dict;

/**
 * Created by akichigi on 19.08.16.
 */
public class AccTypeModifierWrapper extends AccTypeSubProductWrapper {
    private String modifier;
    private String modifierName;

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String name) {
        this.modifierName = name;
    }
}
