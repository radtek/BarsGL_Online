package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.SubProduct;
import ru.rbt.barsgl.ejb.entity.dict.AccType.SubProductId;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeModifierRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeProductRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeSubProductRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccTypeSubProductWrapper;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * Created by akichigi on 23.08.16.
 */
public class AccTypeSubProductController extends BaseDictionaryController<AccTypeSubProductWrapper, SubProductId, SubProduct, AccTypeSubProductRepository> {
    @Inject
    private AccTypeModifierRepository modifierRepository;

    @Inject
    private AccTypeSubProductRepository subProductRepository;

    @Inject
    private AccTypeProductRepository productRepository;

    @Override
    public RpcRes_Base<AccTypeSubProductWrapper> create(AccTypeSubProductWrapper wrapper) {
        if(!productRepository.isProductCodeExists(wrapper.getSection(), wrapper.getProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Продукт '%s' из раздела '%s' не существует!",
                    wrapper.getProduct(), wrapper.getSection()));
        }

        if(subProductRepository.isSubProductCodeExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Подпродукт '%s' в разделе '%s' и продукте '%s' уже существует!",
                    wrapper.getSubProduct(), wrapper.getSection(), wrapper.getProduct()));
        }

        if(subProductRepository.isSubProductNameExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProductName())){
            return new RpcRes_Base<>(wrapper, true, format("Подпродукт c наименованием '%s' в разделе '%s' и продукте '%s' уже существует!",
                    wrapper.getSubProductName(), wrapper.getSection(), wrapper.getProduct()));
        }

        SubProductId primaryKey =  new SubProductId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct());
        return create(wrapper, subProductRepository, SubProduct.class, primaryKey,
                format("Подпродукт '%s' в разделе '%s' и продукте '%s' уже существует!", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("В разделе '%s' и продукте '%s' создан подпродукт '%s'", primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("Ошибка при создании подпродукта '%s' d разделе '%s' и продукте '%s'", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                () -> new SubProduct(primaryKey,
                        wrapper.getSubProductName()));
    }

    @Override
    public RpcRes_Base<AccTypeSubProductWrapper> update(AccTypeSubProductWrapper wrapper) {
        SubProductId primaryKey = new SubProductId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct());

        if(subProductRepository.isSubProductExists(primaryKey, wrapper.getSubProductName())){
            return new RpcRes_Base<>(wrapper, true, format("Подпродукт c наименованием '%s' в разделе '%s' и продукте '%s' уже существует!",
                    wrapper.getSubProductName(), wrapper.getSection(), wrapper.getProduct()));
        }

        return update(wrapper, subProductRepository, SubProduct.class,
                primaryKey,
                format("Подпродукт '%s' в разделе '%s' и продукте '%s' не найден!", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("Изменен подпродукт '%s' из раздела '%s' и продукта '%s'", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("Ошибка при изменении подпродукта '%s' из раздела '%s' и продукта '%s'", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                subproduct -> {
                    subproduct.setName(wrapper.getSubProductName());
                });
    }

    @Override
    public RpcRes_Base<AccTypeSubProductWrapper> delete(AccTypeSubProductWrapper wrapper) {
        if (modifierRepository.isSubProductExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления подпродукта. Подпродукт '%s' из раздела '%s', продукта '%s' содержит модификаторы!",
                   wrapper.getSubProduct() , wrapper.getSection(), wrapper.getProduct()));
        }

        SubProductId primaryKey = new SubProductId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct());

        return delete(wrapper, subProductRepository, SubProduct.class, primaryKey,
                format("Подпродукт '%s' в разделе '%s' и продукте '%s' не найден!", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("Удален подпродукт '%s' из раздела '%s' и продукта '%s'", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode()),
                format("Ошибка при удалении подпродукта '%s' из раздела '%s' и продукта '%s'", primaryKey.getSubprodcode(), primaryKey.getSectcode(), primaryKey.getProdcode())
        );
    }
}
