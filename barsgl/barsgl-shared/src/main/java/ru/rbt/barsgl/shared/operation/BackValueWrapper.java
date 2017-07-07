package ru.rbt.barsgl.shared.operation;

import ru.rbt.barsgl.shared.enums.BackValueAction;
import ru.rbt.barsgl.shared.enums.BackValueMode;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.filter.IFilterItem;

import java.util.List;

/**
 * Created by er18837 on 07.07.2017.
 */
public class BackValueWrapper {

    List<Long> gloIDs;              // видимый список или 1 операция
    List<IFilterItem> filters;      // критерии фильтра
    BackValueAction action;         // действие
    BackValueMode mode;             // режим обработки
    String postDateStr;             // дата проводки
    String comment;                 // Комментарий
    BackValuePostStatus bvStatus;   // текущий статус
}
