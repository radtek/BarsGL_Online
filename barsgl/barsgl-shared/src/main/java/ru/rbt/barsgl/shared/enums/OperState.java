package ru.rbt.barsgl.shared.enums;

/**
 * Created by akichigi on 22.04.15.
 * статус операции
 */
public enum OperState {
     // загружена
    LOAD
    ,
    // загружена как BACK_VALUE
    BLOAD
    ,
    // ошибка данных
    ERCHK
    ,
    // ошибка данных авторизованных BackValue операций
    BERCHK
    ,
    // ссылки найдены (не используется)
    PROC
    ,
    // ошибка определения ссылочных операций
    ERPROC
    ,
    // ожидание прихода аккаунта
    WTAC
    ,
    // ожидание прихода аккаунта для BACK_VALUE
    BWTAC
    ,
    // не пришел аккаунт для BACK_VALUE
    BERWTAC
    ,
    //обработана (созданы проводки)
    POST
    ,
    //ошибка создания проводки
    ERPOST
    ,
    //отменена
    CANC
    ,
    //сторнируемая операция отменена
    SOCANC
    ,
    //операция обработана успешно, проводки созданы, но им выставлен статус INVISIBLE = '1'
    INVISIBLE
}
