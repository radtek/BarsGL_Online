package ru.rbt.barsgl.ejb.repository.loader;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.loader.LoadManagement;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
@Stateless
@LocalBean
public class LoadManagementRepository extends AbstractBaseEntityRepository<LoadManagement, Long> {
}
