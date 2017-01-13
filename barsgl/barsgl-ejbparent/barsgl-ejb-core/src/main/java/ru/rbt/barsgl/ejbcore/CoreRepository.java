package ru.rbt.barsgl.ejbcore;

import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class CoreRepository extends AbstractBaseEntityRepository{}
