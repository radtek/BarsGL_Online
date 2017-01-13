package ru.rbt.barsgl.ejbcore.remote.http;

import java.io.*;

/**
 * Класс-утилита для сериализации объектных данных
 */
public final class Serializer {

    private Serializer() {}

    /**
     * Восстанавливает граф объектов из его текущего сериализованного состояния
     * @param data Сериализованное состояние объекта
     * @return Десериализованный граф объектов
     * @throws IOException В случае ошибки чтения данных
     * @throws ClassNotFoundException В случае отсутствия нужного класса
     */
    public static Object readObject(byte[] data) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        return ois.readObject();
    }

    /**
     * Сериализует граф объектов
     * @param obj Граф объектов
     * @return Сериализованное состояние графа объектов
     * @throws IOException В случае ошибки записи данных
     */
    public static byte[] writeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }

}
