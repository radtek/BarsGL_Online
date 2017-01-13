package ru.rbt.barsgl.ejbcore.datarec;

import java.io.Serializable;

/**
 * Описывает один параметр вызова хранимой процедуры
 */
public final class CallParam implements Serializable {

    /**
     * Константа определяющая тип параметра как входящий
     * @see #getDirection()
     */
    public static final int IN     = 0x01;

    /**
     * Константа определяющая тип параметра как исходящий
     * @see #getDirection()
     */
    public static final int OUT    = 0x02;

    /**
     * Константа определяющая тип параметра как входящий и исходящий
     * @see #getDirection()
     */
    public static final int IN_OUT = 0x03;

    private int _direction;
    private String _name;
    private Object _value;
    private int _type;

    /**
     * Конструктор по умолчанию нужен для сериализации
     */
    protected CallParam() {
    }

    /**
     * Создает новый экземпляр {@link CallParam} с заданным типом и направлением
     * @param direction направление параметра ({@link #IN}, {@link #OUT} или {@link #IN_OUT})
     * @param type SQL-тип панаметра (см. {@link java.sql.Types})
     */
    public CallParam(int direction, int type) {
        if ((direction < 1) || (direction > 3)) {
            throw new IllegalArgumentException("Invalid direction value (must be either IN, OUT or IN_OUT): " + direction);
        }
        _direction = direction;
        _type = type;
    }

    /**
     * Создает новый экземпляр {@link CallParam} с заданным направлением, типом и значением.<br/>
     * Данный конструктор имеет смысл использовать для IN- или IN_OUT-параметров, т.к. для
     * OUT-параметров задание значения не имеет смысла и игнорируется
     * @param direction направление параметра ({@link #IN}, {@link #OUT} или {@link #IN_OUT})
     * @param type SQL-тип параметра (см. {@link java.sql.Types})
     * @param value значение параметра
     */
    public CallParam(int direction, int type, Object value) {
        this(direction, type);
        _value = value;
    }

    /**
     * Создает новый экземпляр {@link CallParam} с заданным направлением, типом и именем.<br/>
     * Данный конструктор имеет смысл использовать только для OUT- или IN_OUT-параметров, когда
     * имя параметра может быть использовано для получения выходного значения после вызова
     * хранимой процедуры. Для IN-параметров имя игнорируется
     * @param name имя параметра
     * @param direction направление параметра ({@link #IN}, {@link #OUT} или {@link #IN_OUT})
     * @param type SQL-тип параметра (см. {@link java.sql.Types})
     */
    public CallParam(String name, int direction, int type) {
        this(direction, type);
        _name = name;
    }

    public CallParam(String name, int direction, int type, Object value) {
        this(direction, type, value);
        _name = name;
    }

    /**
     * Возвращает направление параметра
     * @return Одно из трех значений: {@link #IN}, {@link #OUT} или {@link #IN_OUT}
     */
    public int getDirection() {
        return _direction;
    }

    /**
     * Указывает, является ли параметр исходящим
     * @return <code>true</code>, если параметр имеет направление {@link #IN} или {@link #IN_OUT},
     *     <code>false</code> в противном случае
     */
    public boolean isInput() {
        return (_direction & IN) > 0;
    }

    /**
     * Указывает, является ли параметр исходящим
     * @return <code>true</code>, если параметр имеет направление {@link #OUT} или {@link #IN_OUT},
     *     <code>false</code> в противном случае
     */
    public boolean isOutput() {
        return (_direction & OUT) > 0;
    }

    /**
     * Возвращает SQL-тип параметра
     * @return SQL-тип параметра, в соответствии с {@link java.sql.Types}
     */
    public int getType() {
        return _type;
    }

    /**
     * Возвращает значение параметра
     * @return значение параметра
     */
    public Object getValue() {
        return _value;
    }

    /**
     * Устанавливает значение параметра
     * @param value Новое значение параметра
     */
    public void setValue(Object value) {
        _value = value;
    }

    /**
     * Возвращает имя параметра
     * @return имя паратмера
     */
    public String getName() {
        return _name;
    }
}
