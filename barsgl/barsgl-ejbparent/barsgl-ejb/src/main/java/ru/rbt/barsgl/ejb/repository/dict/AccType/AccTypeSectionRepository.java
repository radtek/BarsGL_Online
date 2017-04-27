package ru.rbt.barsgl.ejb.repository.dict.AccType;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.AccType.Section;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 22.08.16.
 */
@Stateless
@LocalBean
public class AccTypeSectionRepository extends AbstractBaseEntityRepository<Section, String> {
    public boolean isSectionCodeExists(String id) {
        return null != selectFirst(Section.class, "from Section T where T.id = ?1", id);
    }

    public boolean isSectionNameExists(String name) {
        return null != selectFirst(Section.class, "from Section T where T.name = ?1", name);
    }

    public boolean isSectionExists(String id, String name) {
        return null != selectFirst(Section.class, "from Section T where T.name = ?1 and T.id <> ?2",
                name, id);
    }

    public Section getSection(String id){
        return selectFirst(Section.class, "from Section T where T.id = ?1", id);
    }
}
