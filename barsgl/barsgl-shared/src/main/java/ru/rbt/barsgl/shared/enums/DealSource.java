package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 12.11.15.
 */
public enum DealSource {
    KondorPlus("K+TP"), PaymentHub("PH"), Flex12("FC12_CL"), MZO("MZO"), ARMPRO("ARMPRO"), Manual("MANUAL");

    private final String label;

    DealSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
