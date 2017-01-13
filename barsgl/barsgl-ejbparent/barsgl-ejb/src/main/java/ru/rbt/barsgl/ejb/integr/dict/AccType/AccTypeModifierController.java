package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.Modifier;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ModifierId;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeModifierRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeSubProductRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccTypeModifierWrapper;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * Created by akichigi on 24.08.16.
 */
public class AccTypeModifierController extends BaseDictionaryController<AccTypeModifierWrapper, ModifierId, Modifier, AccTypeModifierRepository> {
    @Inject
    private AccTypeModifierRepository modifierRepository;

    @Inject
    private AccTypeRepository accTypeRepository;

    @Inject
    private AccTypeSubProductRepository subProductRepository;


    @Override
    public RpcRes_Base<AccTypeModifierWrapper> create(AccTypeModifierWrapper wrapper) {
        if(!subProductRepository.isSubProductCodeExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct())){
            return new RpcRes_Base<>(wrapper, true, format("Подпродукт '%s' в разделе '%s' и продукте '%s' не существует!",
                    wrapper.getSubProduct(), wrapper.getSection(), wrapper.getProduct()));
        }

        if(modifierRepository.isModifierCodeExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(), wrapper.getModifier())){
            return new RpcRes_Base<>(wrapper, true, format("Модификатор '%s' в разделе '%s', продукте '%s' и подпродукте '%s' уже существует!",
                    wrapper.getModifier(), wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct()));
        }

        if(modifierRepository.isModifierNameExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(), wrapper.getModifierName())){
            return new RpcRes_Base<>(wrapper, true, format("Модификатор c наименованием '%s' в разделе '%s', продукте '%s' и подпродукте '%s' уже существует!",
                    wrapper.getModifierName(), wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct()));
        }

        ModifierId primaryKey = new ModifierId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(), wrapper.getModifier());
        return create(wrapper, modifierRepository, Modifier.class, primaryKey,
                format("Модификатор '%s' в разделе '%s', продукте '%s' и подпродукте '%s' уже существует!", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("В разделе '%s', продукте '%s' и подпродукте '%s' создан модификатор '%s'", primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode(), primaryKey.getModifcode()),
                format("Ошибка при создании модификатора '%s' из раздела '%s', продукта '%s' и подпродукта '%s'", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                () -> new Modifier(primaryKey, wrapper.getModifierName()));
    }

    @Override
    public RpcRes_Base<AccTypeModifierWrapper> update(AccTypeModifierWrapper wrapper) {
        ModifierId primaryKey = new ModifierId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(), wrapper.getModifier());

        if(modifierRepository.isModifierExists(primaryKey, wrapper.getModifierName())){
            return new RpcRes_Base<>(wrapper, true, format("Модификатор c наименованием '%s' в разделе '%s', продукте '%s' и подпродукте '%s' уже существует!",
                    wrapper.getModifierName(), wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct()));
        }

        return update(wrapper, modifierRepository, Modifier.class,
                primaryKey,
                format("Модификатор '%s' в разделе '%s', продукте '%s' и подпродукте '%s' не найден!", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("Изменен модификатор '%s' из раздела '%s', продукта '%s' и подпродукта '%s'", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("Ошибка при изменении модификатора '%s' из раздела '%s', продукта '%s' и подпродукта '%s'", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                modifier -> {
                    modifier.setName(wrapper.getModifierName());
                });
    }

    @Override
    public RpcRes_Base<AccTypeModifierWrapper> delete(AccTypeModifierWrapper wrapper) {
        if (accTypeRepository.isModifierExists(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(),
                wrapper.getModifier())){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления модификатора. Модификатор '%s' входит в состав AccType!",
                    wrapper.getModifier()));
        }

        ModifierId primaryKey = new ModifierId(wrapper.getSection(), wrapper.getProduct(), wrapper.getSubProduct(), wrapper.getModifier());

        return delete(wrapper, modifierRepository, Modifier.class, primaryKey,
                format("Модификатор '%s' в разделе '%s', продукте '%s' и подпродукте '%s' не найден!", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("Удален модификатор '%s' из раздела '%s', продукта '%s' и подпродукта '%s'", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode()),
                format("Ошибка при удалении модификатора '%s' из раздела '%s', продукта '%s' и подпродукта '%s'", primaryKey.getModifcode(),  primaryKey.getSectcode(), primaryKey.getProdcode(), primaryKey.getSubprodcode())
        );
    }
}
