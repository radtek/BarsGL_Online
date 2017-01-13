package ru.rbt.barsgl.ejbcore.remote;

import ru.rbt.barsgl.ejbcore.remote.http.ServiceRequest;
import ru.rbt.barsgl.ejbcore.remote.http.ServiceResponse;

import javax.ejb.Local;
import javax.ejb.Remote;

/**
 * Created by Ivan Sevastyanov
 */
@Remote
@Local
public interface ServerAccess {
    /**
     * для интерфейсного удаленного доступа
     * @param clazz класс интерфеса серверного бина
     * @param method вызываемый метод
     * @param params параметры вызова серверного метода
     * @param <T> параметризуемый метод
     * @return результат
     */
    <T> T invoke(Class clazz, String method, Object ... params) throws Exception;

    /**
     * для улаленного доступа без использования интерфейса на стороне клиента.
     * @param className название класса интерфеса серверного бина
     * @param method вызываемый метод
     * @param params параметры вызова серверного метода
     * @param <T> параметризуемый метод
     * @return результат
     */
    <T> T invoke(String className, String method, Object ... params) throws Exception;

    /**
     * Работа с входными/выходными данными как с массивами байт
     * @param data реально, сериализованный {@link ServiceRequest}
     * @return сериаализованный {@link ServiceResponse}
     */
    byte[] invoke(byte[] data) throws Exception;
}
