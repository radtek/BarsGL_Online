package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

/**
 * Created by Ivan Sevastyanov
 */
public interface EtlMessageController <From, To extends BaseEntity> {

    To processMessage(From etlMessage);

}
