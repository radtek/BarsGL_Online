package ru.rbt.barsgl.gwt.core.resources;

import com.google.gwt.i18n.client.Constants;

/**
 * Created by ER21006 on 13.01.2015.
 */
public abstract interface TextConstants extends Constants {
    @Constants.DefaultStringValue("Клиент")
    public abstract String appName();

    @Constants.DefaultStringValue("Новая запись")
    public abstract String grid_New();

    @Constants.DefaultStringValue("Всего записей:")
    public abstract String grid_lbl_allRows();

    @Constants.DefaultStringValue("Экспорт данных")
    public abstract String exportFrm_header();

    @Constants.DefaultStringValue("Все")
    public abstract String exportFrm_all();

    @Constants.DefaultStringValue("Количество")
    public abstract String exportFrm_count();

    @Constants.DefaultStringValue("Введите количество строк для экспорта")
    public abstract String exportFrm_msg();

    @Constants.DefaultStringValue("Ошибка подготовки CSV-файла")
    public abstract String exportFrm_csvErr();

    @Constants.DefaultStringValue("Фильтр")
    public abstract String filterFrm_header();

    @Constants.DefaultStringValue("Применить")
    public abstract String applyBtn_caption();

    @Constants.DefaultStringValue("Настройки")
    public abstract String settingsFrm_header();

    @Constants.DefaultStringValue("Поиск")
    public abstract String filterFrm_search();

    @Constants.DefaultStringValue("Очистить")
    public abstract String filterFrm_clear();

    @Constants.DefaultStringValue("Добавить новую запись")
    public abstract String recFrm_headerNew();

    @Constants.DefaultStringValue("Редактировать запись")
    public abstract String recFrm_headerEdit();

    @Constants.DefaultStringValue("Сохранить")
    public abstract String btn_Save();

    @Constants.DefaultStringValue("Загрузить")
    public abstract String btn_Load();

    @Constants.DefaultStringValue("Сбросить настройки")
    public abstract String btn_SettingReset();

    @Constants.DefaultStringValue("Экспорт")
    public abstract String btn_Export();

    @Constants.DefaultStringValue("Экспорт для РР")
    public abstract String btn_IssueExport();

    @Constants.DefaultStringValue("Отменить")
    public abstract String btn_cancel();

    @Constants.DefaultStringValue("Войти")
    public abstract String btn_login();

    @Constants.DefaultStringValue("Выход")
    public abstract String btn_exit();

    @Constants.DefaultStringValue("Редактировать")
    public abstract String btn_edit();

    @Constants.DefaultStringValue("Авторизация")
    public abstract String logFrm_info();

    @Constants.DefaultStringValue("Пользователь")
    public abstract String logFrm_login();

    @Constants.DefaultStringValue("Пароль")
    public abstract String logFrm_password();


    @Constants.DefaultStringValue("Ошибка")
    public abstract String logFrm_header_error();


    @Constants.DefaultStringValue("Параметры отчета")
    public abstract String repForm_params();

    @Constants.DefaultStringValue("Основной отчет")
    public abstract String repForm_mainReport();


    @Constants.DefaultStringValue("Приложения и действия")
    public abstract String locationFrm_treeHeader();

    @Constants.DefaultStringValue("Скрыть")
    public abstract String locationFrm_hideTree();

    @Constants.DefaultStringValue("Показать")
    public abstract String locationFrm_showTree();

    @Constants.DefaultStringValue("Паказать панель администратора")
    public abstract String locationFrm_showAdminPanel();

    @Constants.DefaultStringValue("Запомнить")
    public abstract String logFrm_save_data();

    @Constants.DefaultStringValue("Сортировка")
    public abstract String waitMessage_Sort();

    @Constants.DefaultStringValue("Загрузка информации. Пожалуйста, подождите")
    public abstract String waitMessage_Load();

    @Constants.DefaultStringValue("Проверка информации. Пожалуйста, подождите")
    public abstract String waitMessage_Check();

    @Constants.DefaultStringValue("Выполнить")
    public abstract String formData_Exec();

    @Constants.DefaultStringValue("Вставить")
    public abstract String formData_Insert();

    @Constants.DefaultStringValue("Обновить")
    public abstract String formData_Select();

    @Constants.DefaultStringValue("Сохранить")
    public abstract String formData_Update();

    @Constants.DefaultStringValue("Восстановить")
    public abstract String formData_Cancel();

    @Constants.DefaultStringValue("Удалить")
    public abstract String formData_Delete();

    @Constants.DefaultStringValue("Неизвестное действие")
    public abstract String formData_Unknown();

    @Constants.DefaultStringValue("Детали")
    public abstract String btn_Info();

    @Constants.DefaultStringValue("Закрыть")
    public abstract String btn_Close();

    @Constants.DefaultStringValue("Отмена")
    public abstract String formInput_cancel();

    @Constants.DefaultStringValue("Ввод")
    public abstract String formInput_save();

    @Constants.DefaultStringValue("Да")
    public abstract String messageDlg_Yes();

    @Constants.DefaultStringValue("Нет")
    public abstract String messageDlg_No();

    @Constants.DefaultStringValue("Продолжить")
    public abstract String messageDlg_Continue();

    @Constants.DefaultStringValue("Выбрать")
    public abstract String btn_select();

    @Constants.DefaultStringValue("Bars GL")
    public abstract String window_title();

    @Constants.DefaultStringValue("Начало выгрузки таблицы в Excel. Ожидайте...")
    public abstract String export2Excel();
}
