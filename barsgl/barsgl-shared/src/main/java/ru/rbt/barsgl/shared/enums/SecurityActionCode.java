package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER21006 on 19.04.2016.
 */
public enum SecurityActionCode {

    AccOFRLook("Просмотр списка счетов ОФР")
    , AccOFRInp("Ввод счета ОФР")
    , AccOFRChng("Изменение счета ОФР")
    , AccLook("Просмотр списка счетов")
    , AccInp("Ввод счета")
    , AccChng("Изменение счета")
    , AccClose("Закрытие счета")
    , AccRestUnloadRun("Выгрузка остатков бухгалтером")
    , AccOperInp("Ввод операции через счет")
    , OperLook("Просмотр списка операций/проводок")
    , OperInp("Ввод операции")
    , OperInpTmpl("Ввод операции по шаблону")
    , OperPstChng("Изменение операции")
    , OperPstMakeInvisible("Подавление операции")
    , OperFileLoad("Загрузка операций из файла")
    , OperFileLook("Просмотр сообщений из файла")
    , OperAELook("Просмотр сообщений из АЕ")
    , OperErrLook("Просмотр ошибок обработки операций")
    , OperHand2("Подтверждение текущих операций (2 рука)")
    , OperHand3("Подтверждение операций в прошлый день (3 рука)")
    , OperTmplProc("Создание и изменение шаблона операции")
    , OperBackValue("Управление доступом в архив ответственным из бухгалтерии")
    , ReferLook("Просмотр справочников")
    , ReferChng("Изменение справочников, кроме Плана счетов")
    , ReferDel("Удаление записей в справочниках, кроме Плана счетов")
    , ReferAccTypeChng("Изменение Плана счетов AccType  ")
    , ReferAccTypeDel("Удаление записей из Плана счетов AccType")
    , ReferAccSTAMT("Настройка списка счетов для выгрузки в STAMT")
    , UserLook("Просмотр списка пользователей")
    , UserChng("Изменение учетной карточки пользователя")
    , UserCntl("Назначение роли")
    , UserCntlBackValue("Изменение параметров доступа в архив")
    , RoleLook("Просмотр списка ролей")
    , RoleInp("Ввод новой роли")
    , RoleChng("Изменение названия и содержания (функций) роли")
    , TskOdStateLook("Просмотр состояния опердня")
    , TskOdOpenRun("Запуск задания 'Открытие ОД'")
    , TskOdBalCloseRun("Запуск задания 'Закрытие баланса предыдущего ОД'")
    , TskOdPreCobRun("Запуск задания 'Перевод фазы в PRE_COB'")
    , TskOdSwitchModeRun("Запуск задания 'Переключение режима загрузки'")
    , TasksLook("Просмотр списка служб (заданий)")
    , TasksChng("Настройка службы (задания)")
    , TasksRun("Изменение состояния службы (задания), кроме открытия и закрытия ОД")
    , OthAuditLook("AUDIT")
    , OperAcceptance("Операции, ожидающие подтверждение 2-й или 3-й рукой")
    , OperHistory("Просмотр истории создания операций")
    , OperHistoryUser("Просмотр запросов на создание операций")
    , OperToExcel("Выгрузка в Excel")
    , OperPstChngDate("Изменение даты проводки без анализа прав доступа в архив")
    , OperPstChngDateArcRight("Изменение даты проводки с анализом доступа в архив")
    , LoaderControl("Мониторинг и управление загрузкой")
    , LoaderStepActionChg("Изменение действия над шагом загрузки")
    , LoaderStepActionAssign("Назначение действий на шаги загрузки")
    , LoaderStepActionApprove("Согласование действий, назначенных на шаги загрузки")
    , LoaderStepActionExecute("Выполнение действий, назначенных на шаги загрузки")
    , LoaderStepActionCancel("Отмена действий, назначенных на шаги загрузки")
    , TaskMonitor("Мониторинг")
    , OperManualLook("Просмотр списка запросов ручного ввода")
    , Acc707Inp("Ввод счета ОФР прошлых лет")
    , Acc707Chng("Изменение счета ОФР прошлых лет")
    ;

    private final String desc;

    SecurityActionCode(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * parse string
     * @param securityActionCode
     * @return null if IllegalArgumentException
     */
    public static SecurityActionCode parse(String securityActionCode) {
        try {
            return valueOf(securityActionCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
