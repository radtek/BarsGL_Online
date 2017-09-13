package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 12.11.15.
 */
public enum DealSource {
    KondorPlus("K+TP"), PaymentHub("PH"), Flex12("FC12_CL"), MZO("MZO"), ARMPRO("ARMPRO"), Manual("MANUAL"), SECMOD("SECMOD"), AOS("AOS");

    private final String label;

    DealSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean withTechWorkDay(String src) {
        return ARMPRO.getLabel().equals(src);
    }
}
