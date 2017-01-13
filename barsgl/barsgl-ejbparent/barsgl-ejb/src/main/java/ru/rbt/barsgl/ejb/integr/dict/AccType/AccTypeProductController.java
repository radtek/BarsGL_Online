package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.Product;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ProductId;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeProductRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeSectionRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeSubProductRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccTypeProductWrapper;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * Created by akichigi on 23.08.16.
 */
public class AccTypeProductController extends BaseDictionaryController<AccTypeProductWrapper, ProductId, Product, AccTypeProductRepository> {
    @Inject
    private AccTypeProductRepository productRepository;

    @Inject
    private AccTypeSubProductRepository subProductRepository;

    @Inject
    private AccTypeSectionRepository sectionRepository;

    @Override
    public RpcRes_Base<AccTypeProductWrapper> create(AccTypeProductWrapper wrapper) {
        if(!sectionRepository.isSectionCodeExists(wrapper.getSection())){
            return new RpcRes_Base<>(wrapper, true, format("Раздел c кодом '%s' не существует!", wrapper.getSection()));
        }

        if(productRepository.isProductCodeExists(wrapper.getSection(), wrapper.getProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Продукт '%s' из раздела '%s' уже существует!",
                    wrapper.getProduct(), wrapper.getSection()));
        }

        if(productRepository.isProductNameExists(wrapper.getSection(), wrapper.getProductName())){
            return new RpcRes_Base<>(wrapper, true, format("Продукт c наименованием '%s' в разделе '%s' уже существует!",
                    wrapper.getProductName(), wrapper.getSection()));
        }

        ProductId primaryKey =  new ProductId(wrapper.getSection(), wrapper.getProduct());
        return create(wrapper, productRepository, Product.class, primaryKey,
                format("Продукт '%s' в разделе'%s' уже существует!", primaryKey.getProdcode(), primaryKey.getSectcode()),
                format("В разделе '%s' создан продукт '%s'", primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("Ошибка при создании продукта '%s' в разделе'%s'",  primaryKey.getProdcode(), primaryKey.getSectcode()),
                () -> new Product(primaryKey, wrapper.getProductName()));
    }

    @Override
    public RpcRes_Base<AccTypeProductWrapper> update(AccTypeProductWrapper wrapper) {
        ProductId primaryKey =  new ProductId(wrapper.getSection(), wrapper.getProduct());

        if(productRepository.isProductExists(primaryKey, wrapper.getProductName())){
            return new RpcRes_Base<>(wrapper, true, format("Продукт c наименованием '%s' в разделе '%s' уже существует!",
                    wrapper.getProductName(), wrapper.getSection()));
        }

        return update(wrapper, productRepository, Product.class,
                primaryKey,
                format("Продукт '%s' в разделе '%s' не найден!", primaryKey.getProdcode(), primaryKey.getSectcode()),
                format("Изменен продукт '%s' из раздела '%s'", primaryKey.getProdcode(), primaryKey.getSectcode()),
                format("Ошибка при изменении продукта '%s' из раздела '%s'", primaryKey.getProdcode(), primaryKey.getSectcode()),
                product -> {
                    product.setName(wrapper.getProductName());
                });
    }

    @Override
    public RpcRes_Base<AccTypeProductWrapper> delete(AccTypeProductWrapper wrapper) {
        if (subProductRepository.isProductExists(wrapper.getSection(), wrapper.getProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления продукта. Продукт '%s' из раздела '%s' содержит подпродукты!",
                    wrapper.getProduct(), wrapper.getSection()));
        }

        ProductId primaryKey = new ProductId(wrapper.getSection(), wrapper.getProduct());

        return delete(wrapper, productRepository, Product.class, primaryKey,
                format("Продукт '%s' в разделе '%s' не найден!", primaryKey.getProdcode(), primaryKey.getSectcode()),
                format("Удален продукт '%s' из раздела '%s'",  primaryKey.getProdcode(), primaryKey.getSectcode()),
                format("Ошибка при удалении продукта '%s' из раздела '%s'", primaryKey.getProdcode(), primaryKey.getSectcode())
        );
    }
}
