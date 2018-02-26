package ru.rbt.ejbcore.validation;

/**
 * Created by Ivan Sevastyanov
 */
public enum ErrorCode {

    /**
     * Ошибки при валидации EtlPosting и GLOperation
     */
    FIELD_IS_EMPTY(1, "Пустое значение поля: '%s'")
    , STRING_FIELD_IS_TOO_LONG(2, "Неверная длина строки: '%s' (поле '%s', допустимая длина: %s)")
    , ACCOUNT_FORMAT_INVALID(3, "Неверный формат счета %s: '%s' (поле '%s')")
    , ACCOUNT_NOT_FOUND(4, "Счет %s не найден: '%s' (поле '%s')")
    , DEALID_PYMANTREF_IS_EMPTY(5, "Не задан идентификатор проводки ('%s' / '%s')")
    , DEALID_FORMAT_INVALID(6, "Неверный формат идентификатора сделки: '%s' (поле '%s')")
    , STORNO_REF_IS_EMPTY(7, "Для проводки сторно не задана ссылка на сторнируемую проводку (поле '%s')")
    , PARENT_REF_IS_EMPTY(8, "Для веерной проводки не задана ссылка на родительскую проводку (поле '%s')")
    , AMOUNT_INVALID(9, "Неверная сумма в валюте %s: %s (поле '%s')")
    , AMOUNT_RU_INVALID(10, "Неверная сумма в рублях %s: %s (поле '%s')")
    , MFO_CHAPTER_NOT_A(11, "Межфилиальная проводка допустима только для счетов главы 'А'! \nСчет по дебету: %s (поле '%s'), счет по кредиту: %s (поле '%s'),")
    , CURRENCY_CODE_NOT_MATCH_ACCOUNT(12, "Код валюты %s: '%s' не соответствует валюте счета: '%s' (поле '%s')")
    , CURRENCY_CODE_IS_EMPTY(13, "Код валюты %s: '%s' не существует (поле '%s')")
    , ACCOUNT_NOT_DEFINED(14, "Не задан счет %s (поле '%s') и ключи для открытия счета (поле '%s')")
    , ACCOUNTKEY_FORMAT_INVALID(15, "Неверный формат ключей для открытия счета %s: '%s' (поле '%s')")
    , CURRENCY_CODE_NOT_MATCH_ACCKEY(16, "Код валюты %s: '%s' не соответствует валюте ключей счета: '%s' (поле '%s')")
    , ACCOUNTKEY_NOT_NUMBER(17, "Ключи для открытия счета %s, поле '%s': '%s' - должго быть число")
    , DATE_AFTER_OPERDAY(18, "Неверная дата %s: '%s'. Текущий операционный день '%s'")
    , DATE_IS_HOLIDAY(19, "Неверная дата %s: '%s'. Это выходной день")
    , POSTDATE_NOT_VALID(20, "Дата проводки '%s' < даты валютирования '%s'")
    , FILIAL_NOT_VALID(21, "Неверный филиал %s: '%s'. Филиал по счету: '%s'")
    , TOO_MANY_ACCRLN_GL(22, "Найдено более одной записи GL_ACC (контрсчет) филиал '%s', валюта '%s', БС2 '%s'")
    , NOT_FOUND_ACCRLN_GL(23, "Не найдено ни одной записи GL_ACC (контрсчет) филиал '%s', валюта '%s', БС2 '%s'")
    , ACCOUNT_IS_CLOSED(24, "Дата открытия/закрытия счета %s ('%s') не соответствует дате проводки (поле '%s')")
    , ACCOUNT_706_NOT_RUR(25, "Счет %s: валютный счет по доходам-расходам недопустим '%s' (поле '%s')")
    , ACCOUNT_706_PSEUDO(26, "Счет %s: псевдо-счет MIDAS недопустим '%s' (поле '%s')")
    , ACCOUNT_NOT_CORRESP(27, "Неверный счет для корреспонденции '%s' с %s счетом '%s'")
    , ACCOUNT_NOT_CORRECT(28, "Некорректный счет %s: '%s'.\nВыберите счет из справочника по счету 2-го порядка")
    , ACCOUNTS_EQUAL(29, "Счета по дебету и кредиту совпадают: '%s' (поля '%s', '%s'")
    , ACCKEYS_EQUAL(30, "Ключи счета по дебету и кредиту совпадают: '%s' (поля '%s', '%s')")
    , AMOUNT_NOT_EQUAL(31, "Суммы в валюте по дебету %s и кредиту %s не равны (поля '%s', '%s')")
    , ACCOUNT_DB_IS_OPEN_LATER(32, "Счет %s открыт после даты валютирования операции: '%s' (поле '%s')")
    , ACCOUNT_CR_IS_OPEN_LATER(33, "Счет %s открыт после даты валютирования операции: '%s' (поле '%s')")
    , ACCOUNT_IS_CLOSED_BEFOR(34, "Счет %s закрыт: '%s' (поле '%s')")
    , POSTDATE_LT_PARENT(35, "Дата сторно операции '%s' < даты родительской операции '%s' (GLOID = %s)")
    , POSTDATE_GT_STORNO(36, "Дата операции '%s' > даты сторнирующей операции '%s' (GLOID = %s)")
    , OPERATION_HAS_STORNO(37, "Нельзя подавить сторнированную операцию. Подавите сначала операцию сторно: GLOID = %s, статус '%s'")
    , AMOUNT_EQUAL(38, "Валюты по дебету '%s' и кредиту '%s' не равны, а суммы равны: %s  (поля '%s', '%s')")
    , ACCKEY_CURRENCY_NOT_VALID(39, "Неправильный код валюты '%s' в ключах счета. Ожидалось 'RUR'")
    , OPERATION_707_AFTER_SPOD(40, "Нельзя создать операцию по счету '707...' с датой до '%s' и после '%s'")
    , ACCOUNT_TH_ACCKEY_NOT_VALID(41,"Ключ технического счёта по %s не содержит обязательных полей '%s'")
    , ACCOUNT_TH_CBCCN_NOT_EQUALS(42,"Ключ технического счёта по кредиту и дебету содержат различные коды филиалов '%s' != '%s'")
    , ACCOUNT_TH_ССY_NOT_RUR(43,"Код валюты дебета или кредита должны быть равны RUR. '%s', '%s'")
    , ACCOUNT_TH_ACCTYPE_NOT_VALID(44,"Поле AccType по %s содержит неверный код типа счёта '%s'")
    , ACCOUNT_TECH_NOT_CORRECT(45, "Некорректный счет %s: '%s'.")
    , ANY_CURRNCY_IS_RUR(46,"Одна из валют должна быть RUR")
    , ACCOUNT_TH_IS_CLOSED(47, "Счет %s закрыт: '%s' (дата закрытия: '%s')")
    , ACCOUNT_BALANCE_ERROR(48,"Ошибка при проверке балланса...\n  %s")
 
    /**
     * Ошибки при создании проводки (runtime)
     */
    , DATE_NOT_VALID(1001, "Неверная дата %s '%s'. Текущий операционный день '%s' в фазе '%s',\nпредыдущий операционный день '%s' в статусе '%s'")
    , CURRENCY_CODE_NOT_EXISTS(1002, "Код валюты не существует: '%s'")
    , CURRENCY_RATE_NOT_FOUND(1003, "Курс '%s' на дату '%s' не найден")
    , BALANSE_CHAPTER_IS_DIFFERENT(1005, "Разная глава баланса для счета по дебету: '%s' ('%s') и кредиту: '%s' ('%s')")
    , MFO_ACCOUNT_NOT_FOUND(1006, "Не найден межфилиальный счет в валюте '%s' для филиалов по дебету: '%s' и кредиту: '%s'")
    , STORNO_REF_NOT_FOUND(1007, "Не найдена сторнируемая операция в статусе 'LOAD', 'POST', 'WTAC': EVT_ID = '%s', DEAL_ID =  = '%s', PMT_REF = '%s', VDATE = '%s'")
    , STORNO_REF_NOT_VALID(1008, "Сторнируемая операция: GLOID = %s, статус '%s'; статус должен быть '%s'")
    , FAN_IS_ONLY_ONE(1009, "Найдена всего одна операция веера для PAR_RF = '%s'")
    , FAN_PARENT_NOT_EXISTS(1010, "Не найдена основная операция веера для PAR_RF = '%s'")
    , FAN_SIDE_NOT_DEFINED(1011, "Не удалось определить сторону веера для PAR_RF = '%s'")
//    , FAN_AMOUNT_NOT_DEFINED(1011, "Не удалось определить суммы веера для PAR_RF = '%s'")
    , EXCH_ACCOUNT_NOT_FOUND(1012, "Не найден счет курсовой разницы '%s' для бранча: '%s'")
    , FILIAL_NOT_DEFINED(1013, "Не удалось определить филиал для счета %: '%s' (поле '%s')")
    , STORNO_POST_NOT_FOUND(1014, "Не найдена сторнируемая проводка по операции STRN_GLO = '%s', POST_TYPE = '%s'")
    , FAN_PARENT_NOT_SINGLE(1015, "Найдено более одной основной операции веера для PAR_RF = %s")
    , STORNO_ACCOUNT_NOT_FOUND(1016, "Не найден счет %s для операции сторно: ID_PST = '%s', DEAL_ID =  = '%s'")
    , STORNO_POST_NOT_FOUND_BUFFER(1017, "%s")
    , BALANSE_SECOND_NOT_EXISTS(1018, "Балансовый счет 2-го порядка для счета %s: '%s' не существует")
    , FAN_INVALID_STATE(1019, "Для референса '%s' найдены частичные веерные операции не в статусе '%s': '%s'")
    , FIELDS_DEAL_SUBDEAL(1020, "Не соответствие DealId/SudDealId данным в таблице gl_acc")

    /**
     * Ошибки при валидации и создании счета
     */
//    , ACCOUNT_ALREADY_EXISTS(2001, "Счет ЦБ уже существует в таблице BSAACC: '%s'")
//    , ACCOUNT_MIDAS_ALREADY_EXISTS(2002, "Счет Midas уже существует в таблице ACC: '%s'")
    , CUSTOMER_FORMAT_INVALID(2003, "Неверный формат номера клиента: '%s' (поле '%s')")
    // Ключи счета
    , ACCOUNT_KEY_FORMAT_INVALID(2004, "Ключи счета %s: Неверное значение ключа '%s': '%s' (поле '%s')")
    , CUSTOMER_NUMBER_NOT_FOUND(2005, "Ключи счета %s: Клиент с номером '%s' не найден (поле '%s')")
    , FILIAL_NOT_FOUND(2006, "Ключи счета %s: Филиал не найден: '%s' (поле '%s')")
    , BRANCH_NOT_FOUND(2007, "Ключи счета %s: Бранч не найден: '%s' (поле '%s')")
    , COMPANY_CODE_NOT_VALID(2008, "Ключи счета %s: Код филиала '%s' (поле '%s') не соответствует бранчу '%s' (поле '%s')")
    , COMPANY_CODE_NOT_FOUND(2009, "%s")
    , TOO_MANY_ACCRLN_ENTRIES(2010, "Найдено более одного счета ЦБ '%s' по счету Midas '%s' для '%s'")
    , ACCOUNT_PARAMS_NOT_FOUND(2011, "Ключи счета %s: Не найдены настройки (GL_ACTPARM) для AccountType = '%s', CustomerType = '%s', Term = '%s' на дату '%s'")
    , ACCOUNT_TYPE_IS_NOT_NUMBER(2012, "Ключи счета %s: Неверный формат типа счета (не число): '%s' (поле '%s')")
    , ACCOUNT_TYPE_INVALID(2013, "Ключи счета %s: Тип счета не задан в системе: '%s' (поле '%s')")
    , ACCOUNTGL_ALREADY_EXISTS(2014, "Счет BarsGL с таким набором ключей уже существует в таблице GL_ACC\n<pre>Счет ЦБ:      %s</pre><pre>Счет Midas:   %s</pre>")
    , ACCOUNTGLTH_ALREADY_EXISTS(2060, "Технический счет BarsGL с таким набором ключей уже существует\n<pre>Счет ЦБ:      %s</pre>")
    , CLOSEDATE_NOT_VALID(2015, "Дата закрытия счета '%s' < даты открытия '%s'")
//    , ACCOUNT_RLN_INVALID(2016, "Не найдена запись в таблице ACCRLN для BSAACID = '%s', ACID = '%s'")
//    , ACCOUNT_BSA_INVALID(2017, "Не найдена запись в таблице BSAACC для ID (BSAACID) = '%s'")
    , BALANCE_NOT_ZERO(2018, "Баланс счета '%s' не нулевой")
    , ACCOUNT_IN_USE_AFTER(2019, "По счету '%s' есть операции: %s (необработанные или после даты '%s') ")
    , ACCOUNT2_NOT_VALID(2020, "Ключи счета %s: Неверный балансовый счет 2-го порядка '%s', должен быть '%s'\n для AccountType = '%s', CustomerType = '%s', Term = '%s' (поле %s)")
    , MIDAS_PARAMS_NOT_VALID(2021, "Ключи счета %s: Неверные параметры Midas: '%s' '%s', должны быть '%s' '%s'\n для AccountType = '%s', CustomerType = '%s', Term = '%s' (поля %s)")
    , BRANCH_FLEX_NOT_FOUND(2022, "Ключи счета %s: Бранч FLEX не найден: '%s' (поле '%s')")
    , CUST_TYPE_IS_NOT_NUMBER(2023, "Ключи счета %s: Неверны формат типа собственности клиента (не число): '%s' (поле '%s')")
    , TERM_IS_NOT_NUMBER(2024, "Ключи счета %s: Неверны формат кода срока (не число): '%s' (поле '%s')")
    , MIDAS_SQ_IS_DIFFERENT(2025, "Ключи счета %s: Значение Midas SQ: '%s' не совпадает с предыдущим значением SQ: '%s' для сделки: '%s'")
    , SUBDEAL_ID_IS_EMPTY(2026, "Ключи счета %s: Не задан номер субсделки")
    , SUBDEAL_ID_NOT_EMPTY(2027, "Ключи счета %s: Задан номер субсделки, но не задан номер сделки")

    , ACCOUNT_MIDAS_NOT_FOUND(2028, "Сформированный по заданным параметрам счет Midas не найден: '%s'")
    , PLCODE_NOT_FOUND(2029, "По заданным параметрам символ ОФР не найден: ACOD: '%s', SQ: '%s', Тип собственности: '%s'")
    , ACCOUNT_OFR_ALREADY_EXISTS(2030, "Счет ОФР с таким набором ключей уже существует: '%s'")
    , ACCOUNT_OFR_NOT_EXISTS(2031, "Ошибка при создании счета ОФР: '%s'")
    , PLCODE_NOT_FOUND_7903(2032, "По заданным параметрам символ ОФР не определён: ACOD: '%s'")
    , DEAL_ID_IS_EMPTY(2033, "Ключи счета %s: Не задан номер сделки")
    , DEAL_ID_NOT_FOUND(2034, "Сделка с таким номер не существует: '%s'")
    , ACCOUNTEX_PARAMS_NOT_FOUND(2035, "Ключи счета %s: Не найдены настройки (EXCACPARM) для CCY = '%s', OPTYPE = '%s', PSAV = '%s'")
    , ACCOUNT_IN_USE_BEFORE(2036, "По счету '%s' есть операции: %s (до даты '%s') ")
    , ACCOUNTING_TYPE_NOT_FOUND(2037, "Не найдена запись GL_ACTNAME по ACCTYPE: '%s'")
    , ACCOUNT_IS_CONTROLABLE(2038, "AccountingType '%s' недопустим: соответствует счетам, контролируемым АБС")
    , ACCOUNT_PL_ALREADY_EXISTS(2039, "По заданным параметрам найден счет в таблице ACCRLN:\n" +
            "<pre>Счет ЦБ:      %s</pre><pre>Счет Midas:   %s</pre>\n" +
            "Счет не мигрирован.\nДля миграции счета: предоставьте данные – счет и Accounting Type\n" +
            "Для открытия нового счета: измените настройки Accounting Type '%s'")
    , ACCOUNT_707_AFTER_SPOD(2040, "Нельзя открыть счет '707..' в текущем отчетном периоде\n" +
            "Период учета операций по СПОД завершен '%s'")
    , ACCOUNT_707_BEFORE_446p(2041, "Нельзя открыть счет '707..' с датой открытия ранее \nначала действия Положения ЦБ 446-П: '%s'")
    , ACCOUNT_707_BAD_BRANCH(2042, "Счет '707...' можно открыть только в головном отделении!")
    , PLCODE_NOT_CORRECT(2043, "Символ доходов / расходов '%s' не соответсвует настройкам для:\nAccountType '%s', Тип собств '%s', Код срока '%s', должан быть '%s'")
    , ACCOUNT2_NOT_CORRECT(2044, "Балансовый счет 2-го порядка '%s' не соответсвует настройкам для:\nAccountType '%s', Тип собств '%s', Код срока '%s', должан быть '%s'")
//    GL_SEQ = XX
    , GL_SEQ_XX_KEY_WITH_DEAL(2045, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s некорректно, DEALID д.б.пустым")
    , GL_SEQ_XX_KEY_WITH_SUBDEAL(2046, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, SUBDEAL=%s, GL_SEQ=%s некорректно, SUBDEAL д.б.пустым")
    , GL_SEQ_XX_KEY_WITH_PLCODE(2047, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s некорректно, PLCODE д.б.пустым")
    , GL_SEQ_XX_GL_ACC_NOT_FOUND(2048,"Счет %s с ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s не определяется однозначно, GL_ACC.ACID=%s")
    , GL_SEQ_XX_ACCRLN_NOT_FOUND(2049, "Счет %s с ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s не определяется однозначно, ACCRLN.ACID=%s")
    , GL_SEQ_XX_KEY_WITH_DB_PLCODE(2050, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s некорректно, PLCODE в таблице GL_ACTPARM д.б.пустым")
    , GL_SEQ_XX_KEY_WITH_SQ_0(2051, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s некорректно, SQ=0")
    , GL_SEQ_XX_KEY_WITH_FL_CTRL(2052, "Счет %s задан ключом ACCTYPE=%s, CUSTNO=%s, ACOD=%s, SQ=%s, DEALID=%s, PLCODE=%s, GL_SEQ=%s некорректно, GL_ACTNAME.FL_CTRL=Y")
    , CLIENT_NOT_FOUND(2053, "%")

    // Опердень и задачи
    , OPEN_OPERDAY_ERROR(3001, "%s")
    , CLOSE_OPERDAY_ERROR(3002, "%s")
    , OPERDAY_STATE_INVALID(3003, "Операционный день в недопустимом статусе: '%s', ожидалось '%s'")
    , OPERDAY_LDR_STEP_ERR(3004, "Не закончен или отсутствует шаг загрузки '%s' за дату '%s'")
    , OPERDAY_TASK_ALREADY_EXC(3005, "Задача '%s' уже была выполнена успешно за ОД '%s'")
    , OPERDAY_TASK_ALREADY_RUN(3006, "Задача '%s' уже выполняется в ОД '%s'")
    , STAMT_INCR_DELTA(3007, "Невозможно выгрузить инкремент backvalue: %s")
    , STAMT_DELTA_ERR(3008, "Невозможно выгрузить остатки в STAMT (Step 2): %s")
    , ALREADY_UNLOADED(3009, "%s")
    , PRE_TASK_NOT_COMPLETED(3010, "Не было выгрузки %s за дату %s")
    , IS_NOT_ENOUTH_DATA(3011, "Недостаточно данных для выгрузки: %s")
    , OPERDAY_LDR_STEP_ABSENT(3012, "Отсутствует ожидающий шаг загрузки '%s' за дату '%s'")
    , TASK_ERROR(3013, "%s")
    , TERM_TIMEOUT(3014, "Превышено время ожидания остановки обработки задача '%s'")
    , COB_STEP_ERROR(3015, "%s")
    , COB_IS_RUNNING(3016, "Задача COB за дату '%s' выполняется. Нельзя выполнить расчет")
    , STAMT_UNLOAD_DELETED(3017, "%s")

    , OPERDAY_NOT_ONLINE(3020, "%s. Операционный день в статусе: '%s'")
    , OPERDAY_IN_SYNCHRO(3021, "%s. Выполняется синхронизация проводок,\n повторите попытку через несколько минут")

    // Авторизация и ручная обработка
    , BAD_DATE_FORMAT(4000, "Неверный формат поля '%s': %s")
    , AUTH_LOGIN_FAILED(4001, "Ошибка авторизации пользователя '%s'. Ошибка: %s")
    , POSTING_BACK_NOT_ALLOWED(4002, "У Вас нет прав для работы в закрытом опердне")
    , POSTING_BACK_NOT_IN_DATE(4003, "Вам разрешено работать в закрытом опердне только с %s %s")
    , POSTING_BACK_NOT_IN_DAYS(4004, "Вам разрешен доступ к закрытому опердню только с %s")
    , POSTING_FILIAL_NOT_ALLOWED(4005, "У Вас нет достаточных прав для работы с выбранными филиалами: '%s', '%s'")
    , POSTING_SAME_NOT_ALLOWED(4006, "У Вас нет права авторизовать свой запрос на операцию ID = %s\nЭто может сделать другой пользователь")
    , PACKAGE_FILIAL_NOT_ALLOWED(4007, "У Вас нет достаточных прав для работы с филиалами в пакете")
    , PACKAGE_SAME_NOT_ALLOWED(4008, "У Вас нет права авторизовать свой пакет на операцию ID = %s\nЭто может сделать другой пользователь")
    , MOVEMENT_ERROR(4009, "Ошибка при обращении к сервису движений по запросу ID = %s:\n'%s'")
    , PACKAGE_IS_PROCESSED(4010, "В пакете ID = %s нет запросов на операцию в статусе '%s'")
    , PACKAGE_IS_WORKING(4011, "Пакет ID = %s уже находится в обработке (статус '%s')")
    , POSTING_IS_WORKING(4012, "Запрос на операцию ID = %s уже находится в обработке (статус '%s')")
    , OPER_HAS_MOVEMENT(4013, "Нельзя подавить проводки - по операции '%s' было обращение в сервис движений: '%s'")
    , PACKAGE_BAD_STATUS(4014, "Пакет ID = %s: нельзя '%s' пакет в статусе: '%s' ('%s'). Обновите информацию")
    , POSTING_BACK_GT_30(4015, "Нельзя установить дату ранее чем %s (30 дней от текущего опердня)")
    , POSTINGS_CONTROLLABLE(4016, "%s.\nВ операции есть проводки по контролируемым счетам: '%s'")
    , ACCOUNT707_INP_NOT_ALLOWED(4017, "У Вас нет прав для открытия счетов ОФР прошлых лет")
    , ACCOUNT707_CHNG_NOT_ALLOWED(4018, "У Вас нет прав для изменения счетов ОФР прошлых лет")
    , ACCOUNT706_INP_NOT_ALLOWED(4019, "У Вас нет прав для открытия счетов ОФР текущего периода")
    , ACCOUNT706_CHNG_NOT_ALLOWED(4020, "У Вас нет прав для изменения счетов ОФР текущего периода")
    , ACCOUNT_INP_NOT_ALLOWED(4021, "У Вас нет прав для открытия счетов данной категории")
    , ACCOUNT_CHNG_NOT_ALLOWED(4022, "У Вас нет прав для изменения счетов данной категории")
    , POSTING_STATUS_WRONG(4023, "Статус запроса на операцию не соответствует ожидаемому: '%s' (%s)")
    , PACKAGE_STATUS_WRONG(4024, "Статус пакета не соответствует ожидаемому: '%s' (%s)")
    , REPROCESS_ERROR(4025, "%s")
    , BV_MANUAL_ERROR(4026, "%s")

//    , VALIDATION_ERROR(10000, "%s %s %s")
    ;

    private final int errorCode;
    private final String rawMessage;

    private ErrorCode(int errorCode, String rawMessage) {
        this.errorCode = errorCode;
        this.rawMessage = rawMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
    public String getStrErrorCode() {
        return Integer.toString(errorCode);
    }

    public String getRawMessage() {
        return rawMessage;
    }
}
