package ru.rbt.shared.enums;

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
    , OperHand3Super("Подтверждение операций в прошлый день, включая закрытый период (супер 3 рука)")
    , OperBackValueLook("Просмот ручной авторизации BackValue")
    , OperTmplProc("Создание и изменение шаблона операции")
    , OperBackValue("Управление доступом в архив ответственным из бухгалтерии")
    , ReferLook("Просмотр справочников")
    , ReferChng("Изменение справочников, кроме Плана счетов")
    , ReferDel("Удаление записей в справочниках, кроме Плана счетов")
    , ReferAccTypeChng("Изменение Плана счетов AccType  ")
    , ReferAccTypeDel("Удаление записей из Плана счетов AccType")
    , ReferAccSTAMT("Настройка списка счетов для выгрузки в STAMT")
    , ReferBackValue("Настройка доступа к BackValue")
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
    , TskOdAccessModeSwitch("Переключение режима доступа к системе")
    , TskStopStart("Остановки обработки проводок (перед установкой поставок на BarsGL Oracle)")
    , TskRefreshRest("Управления режимом обновления остатков")
    , SessionLook("Просмотр сессий пользователей")
    , SessionKill("Закрытие сессии пользователя")
    , SessionsKill("Закрытие всех сессий")
    , UserRestrictedAccess("Работа в режиме ограниченного доступа")
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
    , OperErrProc("Переобработка ошибок АЕ")
    , OperErrClose("Закрытие ошибок АЕ")
   /* , ToolsErrLook("Просмотр настройки по обработке ошибок")
    , ToolsErrChng("Изменение настройки по обработке ошибок")*/
    , TechAccLook("Просмотр списка техсчетов")
    , TechAccInp("Открытие техсчета")
    , TechAccChng("Редактирование реквизитов техсчета")
    , TechAccClose("Закрытие техсчета")
    , TechAccOperInp("Ввод операции из списка технических счетов")
    , TechOperLook("Просмотр проводок по техсчетам")
    , TechOperPstChng("Изменение реквизитов проводки по техсчету")
    , TechOperPstMakeInvisible("Подавление проводки по техсчету")
    , TechOperManualLook("Просмотр ручных операций тек.дня по техсчетам")
    , TechOperInp("Ввод операции по техсчету")
    , TechOperHand2("Авторизация текущих операций по техсчетам (2 рука)")
    , TechOperHand3("Авторизация операций backvalue по техсчетам (3 рука)")
    , TechOperHistory("История создания операций по техсчетам для admin")
    , TechOperHistoryUser("История создания операций по техсчетам для user")
    , TechOperPstChngDate("Изменение даты проводки по техсчетам без анализа прав доступа в архив")
    , TechOperPstChngSuper("Изменение реквизитов проводки по техсчету, включая закрытый период")
    , Replication("Ручная репликация")
    , ReferAcc2Deals("Настройка списка счетов для контроля сделки")

    , AccPkgFileLoad("Загрузка пакетов с запросами по счетам")
    , AccPkgFileDel("Удаление пакетов с запросами по счетам")
    , AccPkgFileOpen("Открытие счетов пакетной загрузки")
    , AccPkgFileLook("Просмотр пакетов для загрузки счетов")
    , AccountFileLook("Просмотр счетов пакетной загрузки")

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
