package ru.rbt.barsgl.ejbcore.remote.http;

import ru.rbt.barsgl.ejbcore.util.reflection.TypedValue;

import java.io.Serializable;

/**
 * Запрос на вызов определенного метода заданного сервиса.
 * Используется для осуществления удаленных вызовов сервисов.
 * @see ServiceResponse
 */
public final class ServiceRequest implements Serializable {

    private String service;
    private String method;
    private TypedValue[] args;
    private String sid;
    private ClientParameters clientParameters;

    /**
     * Создает новый экземпляр {@link ServiceRequest}
     * @param service Имя удаленного сервиса для вызова
     * @param method Имя метода сервиса, который необходимо вызвать. Данный метод должен быть публичным и
     * быть описан в интерфейсе сервиса
     * @param sid Идентификатор текущей сессии пользорвателя
     * @param args Аргументы для вызова метода
     */
    public ServiceRequest(String service, String method, TypedValue[] args, String sid, ClientParameters clientParameters) {
        // save parameters
        this.service = service;
        this.method = method;
        this.args = args;
        this.sid = sid;
        this.clientParameters = clientParameters;
    }

    /**
     * Возвращает имя удаленного сервиса
     * @return Имя удаленного сервиса
     */
    public String getServiceName() {
        return service;
    }

    /**
     * Возвращает имя метода
     * @return имя метода
     */
    public String getMethodName() {
        return method;
    }

    /**
     * Возвращает аргументы для вызова метода
     * @return Массив типизированных аргументов для вызова метода
     */
    public TypedValue[] getArguments() {
        return args;
    }

    /**
     * Возвращает идентификатор сессии пользователя, в контексте которой выполняется запрос
     * @return Идентификатор сессии пользователя
     */
    public String getSessionId() {
        return sid;
    }

    public ClientParameters getClientParameters() {
        return clientParameters;
    }
}
