package ru.rbt.barsgl.ejb.repository.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.SubProduct;
import ru.rbt.barsgl.ejb.entity.dict.AccType.SubProductId;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 23.08.16.
 */
public class AccTypeSubProductRepository extends AbstractBaseEntityRepository<SubProduct, SubProductId> {
    public boolean isProductExists(String section, String product) {
        return null != selectFirst(SubProduct.class, "from SubProduct T where T.id.sectcode = ?1 and T.id.prodcode =?2",
                section, product);
    }
    public boolean isSubProductCodeExists(String section, String product, String subproduct) {
        return null != selectFirst(SubProduct.class, "from SubProduct T where T.id.sectcode = ?1 and T.id.prodcode = ?2 and T.id.subprodcode =?3",
                section, product, subproduct);
    }

    //Уникальность наименования в пределах секции и продукта
    public boolean isSubProductNameExists(String section, String product, String name) {
        return null != selectFirst(SubProduct.class, "from SubProduct T where T.id.sectcode =?1  and T.id.prodcode = ?2 and T.name = ?3",
                section, product, name);
    }
    //Уникальность наименования в пределах секции и продукта
    public boolean isSubProductExists(SubProductId id, String name) {
        return null != selectFirst(SubProduct.class, "from SubProduct T where T.id.sectcode =?1 and T.id.prodcode =?2 and T.id.subprodcode <>?3 and T.name = ?4",
                id.getSectcode(), id.getProdcode(), id.getSubprodcode(), name);
    }

    public SubProduct getSubProduct(String section, String product, String subproduct) {
        return selectFirst(SubProduct.class, "from SubProduct T where T.id.sectcode = ?1 and T.id.prodcode = ?2 and T.id.subprodcode =?3",
                section, product, subproduct);
    }
}
