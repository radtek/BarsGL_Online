package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 12.11.15.
 */
public enum DealSource {
    KP("K+TP"), FCC("FC12_CL"), MZO("MZO"), ARMPRO("ARMPRO"), PH("PH"), MNL("MANUAL");

    private final String label;

    private DealSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
