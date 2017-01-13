package ru.rbt.barsgl.ejbtest.utl.support;


import ru.rbt.barsgl.shared.HasLabel;

import java.util.Date;

public class SomeAcc {

    public enum BOOLEAN  {

        TRUE(true), FALSE(false);

        private final boolean value;

        private BOOLEAN(boolean value) {
            this.value = value;
        }

    }

    public enum SIDES implements HasLabel {
        America("Америка"), Argentina("Аргентина")
        , Portugalia("Португалия")
        , Nikaragua("Никарагуа"), Laos("Лаос")
        , Gonduras("Гондурас"), Kampuchia("Кампучия")
        , Angola("Ангола"), Sinegal("Синегал")
        , Chilli("Чили"), Pakistan("Пакистан"),
        Vietnam("Вьетнам"), China("Китай");
        private String label;

        SIDES(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private int id;
    private String number;
    private String owner;
    private Boolean foreign;
    private double preSumDouble;
    private float preSumFloat;
    private Date dateOpen;
    private String accNum;
    private Boolean open;
    private SIDES side;


    public SomeAcc() {
    }

    public SomeAcc(int id, String number, String owner, boolean foreign, double preSumDouble, float preSumFloat, Date dateOpen, String accNum, Boolean open) {
        this.id = id;
        this.number = number;
        this.owner = owner;
        this.foreign = foreign;
        this.preSumDouble = preSumDouble;
        this.preSumFloat = preSumFloat;
        this.dateOpen = dateOpen;
        this.accNum = accNum;
        this.open = open;
    }

    public SomeAcc(int id, String owner, Date dateOpen, String accNum) {
        this(id, null, owner, false, 0.0, 0.0f, dateOpen, accNum, null);
    }

    public String getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Boolean isForeign() {
        return foreign;
    }

    public void setForeign(Boolean foreign) {
        this.foreign = foreign;
    }

    public double getPreSumDouble() {
        return preSumDouble;
    }

    public float getPreSumFloat() {
        return preSumFloat;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public String getAccNum() {
        return accNum;
    }

    public Boolean isOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public SIDES getSide() {
        return side;
    }

    public void setSide(SIDES side) {
        this.side = side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SomeAcc account = (SomeAcc) o;

        if (id != account.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public void setPreSumDouble(double preSumDouble) {
        this.preSumDouble = preSumDouble;
    }

    public void setPreSumFloat(float preSumFloat) {
        this.preSumFloat = preSumFloat;
    }
}
