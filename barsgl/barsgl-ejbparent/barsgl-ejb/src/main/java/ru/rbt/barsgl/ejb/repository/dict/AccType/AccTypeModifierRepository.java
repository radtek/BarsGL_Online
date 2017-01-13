package ru.rbt.barsgl.ejb.repository.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.Modifier;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ModifierId;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 24.08.16.
 */
public class AccTypeModifierRepository extends AbstractBaseEntityRepository<Modifier, ModifierId> {
    public boolean isSubProductExists(String section, String product, String subproduct) {
        return null != selectFirst(Modifier.class, "from Modifier T where T.id.sectcode = ?1 and T.id.prodcode =?2 and T.id.subprodcode =?3",
                section, product, subproduct);
    }

    public boolean isModifierCodeExists(String section, String product, String subproduct, String modifier) {
        return null != selectFirst(Modifier.class, "from Modifier T where T.id.sectcode = ?1 and T.id.prodcode = ?2 and T.id.subprodcode =?3 and T.id.modifcode =?4",
                section, product, subproduct, modifier);
    }

    //Уникальность наименования в пределах секции, продукта и подпродукта
    public boolean isModifierNameExists(String section, String product, String subproduct, String name) {
        return null != selectFirst(Modifier.class, "from Modifier T where T.id.sectcode =?1  and T.id.prodcode = ?2 and T.id.subprodcode =?3 and T.name = ?4",
                section, product, subproduct, name);
    }
    //Уникальность наименования в пределах секции, продукта и подпродукта
    public boolean isModifierExists(ModifierId id, String name) {
        return null != selectFirst(Modifier.class, "from Modifier T where T.id.sectcode =?1 and T.id.prodcode =?2 and T.id.subprodcode =?3 and T.id.modifcode <>?4 and T.name = ?5",
                id.getSectcode(), id.getProdcode(), id.getSubprodcode(), id.getModifcode(), name);
    }

    public Modifier getModifier(String section, String product, String subproduct, String modifier) {
        return selectFirst(Modifier.class, "from Modifier T where T.id.sectcode = ?1 and T.id.prodcode = ?2 and T.id.subprodcode =?3 and T.id.modifcode =?4",
                section, product, subproduct, modifier);
    }
}
