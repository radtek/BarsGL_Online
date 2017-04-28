package ru.rbt.security.ejb.repository.access;

import ru.rbt.security.entity.AppUser;
import ru.rbt.security.entity.access.SecurityAction;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ivan Sevastyanov on 19.04.2016.
 */
public class SecurityActionRepository extends AbstractBaseEntityRepository<SecurityAction,Integer> {

    @Inject
    private TextResourceController textResourceController;
    
    public List<SecurityActionCode> getAvailableActions(AppUser user) {
        return getAvailableActions(user.getId());
    }

    public List<SecurityActionCode> getAvailableActions(Long userId) {
        try {
            return select(textResourceController.getContent("ru/rbt/barsgl/ejb/repository/access/available_actions.sql")
                    , new Object[]{userId}).stream()
                    .filter(rec -> null != SecurityActionCode.parse(rec.getString("ACT_CODE")))
                    .map(rec -> SecurityActionCode.parse(rec.getString("ACT_CODE"))).collect(Collectors.toList());
        } catch (Exception e) {
            throw new DefaultApplicationException(e);
        }
    }

}
