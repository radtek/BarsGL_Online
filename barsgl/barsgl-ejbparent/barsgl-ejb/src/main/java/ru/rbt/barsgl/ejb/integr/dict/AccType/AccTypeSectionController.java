package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.Section;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeProductRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeSectionRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccTypeSectionWrapper;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * Created by akichigi on 22.08.16.
 */
public class AccTypeSectionController extends BaseDictionaryController<AccTypeSectionWrapper, String, Section, AccTypeSectionRepository> {
    @Inject
    private AccTypeSectionRepository sectionRepository;

    @Inject
    private AccTypeProductRepository productRepository;

    @Override
    public RpcRes_Base<AccTypeSectionWrapper> create(AccTypeSectionWrapper wrapper) {
        if(sectionRepository.isSectionCodeExists(wrapper.getSection())){
            return new RpcRes_Base<>(wrapper, true, format("Раздел c кодом '%s' уже существует!", wrapper.getSection()));
        }

        if(sectionRepository.isSectionNameExists(wrapper.getSectionName())){
            return new RpcRes_Base<>(wrapper, true, format("Раздел c наименованием '%s' уже существует!", wrapper.getSectionName()));
        }

        String primaryKey = wrapper.getSection();
        return create(wrapper, sectionRepository, Section.class, primaryKey,
                format("Раздел '%s' уже существует!", primaryKey),
                format("Создан раздел: '%s'", primaryKey),
                format("Ошибка при создании раздела: '%s'", primaryKey),
                () -> new Section(primaryKey, wrapper.getSectionName()));
    }

    @Override
    public RpcRes_Base<AccTypeSectionWrapper> update(AccTypeSectionWrapper wrapper) {
        if(sectionRepository.isSectionExists(wrapper.getSection(), wrapper.getSectionName())){
            return new RpcRes_Base<>(wrapper, true, format("Раздел c наименованием '%s' уже существует!", wrapper.getSectionName()));
        }

        String primaryKey = wrapper.getSection();

        return update(wrapper, sectionRepository, Section.class,
                primaryKey,
                format("Раздел '%s' не найден!", primaryKey),
                format("Изменен раздел: '%s'", primaryKey),
                format("Ошибка при изменении раздела: '%s'", primaryKey),
                section -> {
                    section.setName(wrapper.getSectionName());
                });
    }

    @Override
    public RpcRes_Base<AccTypeSectionWrapper> delete(AccTypeSectionWrapper wrapper) {
        if (productRepository.isSectionExists(wrapper.getSection())){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления раздела. Раздел '%s' содержит продукты!", wrapper.getSection()));
        }

        String primaryKey = wrapper.getSection();

        return delete(wrapper, sectionRepository, Section.class, primaryKey,
                format("Раздел '%s' не найден!", primaryKey),
                format("Удален раздел: '%s'", primaryKey),
                format("Ошибка при удалении раздела: '%s'", primaryKey)
        );
    }
}
