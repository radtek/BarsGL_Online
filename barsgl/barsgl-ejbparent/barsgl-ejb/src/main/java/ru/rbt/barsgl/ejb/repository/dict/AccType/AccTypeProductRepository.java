package ru.rbt.barsgl.ejb.repository.dict.AccType;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.AccType.Product;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ProductId;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 22.08.16.
 */
@Stateless
@LocalBean
public class AccTypeProductRepository extends AbstractBaseEntityRepository<Product, ProductId> {
    public boolean isSectionExists(String section) {
        return null != selectFirst(Product.class, "from Product T where T.id.sectcode = ?1", section);
    }

    public boolean isProductCodeExists(String section, String product) {
        return null != selectFirst(Product.class, "from Product T where T.id.sectcode = ?1 and T.id.prodcode = ?2",
                section, product);
    }

    //Уникальность наименования в пределах секции
    public boolean isProductNameExists(String section, String name) {
        return null != selectFirst(Product.class, "from Product T where T.id.sectcode =?1 and T.name = ?2", section, name);
    }
    //Уникальность наименования в пределах секции
    public boolean isProductExists(ProductId id, String name) {
        return null != selectFirst(Product.class, "from Product T where T.id.sectcode =?1 and T.id.prodcode <> ?2 and T.name = ?3",
               id.getSectcode(), id.getProdcode(), name);
    }

    public Product getProduct(String section, String product) {
        return  selectFirst(Product.class, "from Product T where T.id.sectcode = ?1 and T.id.prodcode = ?2", section, product);
    }
}
