package ru.rbt.security.ejb.repository.access;

import ru.rbt.security.entity.AppUser;
import ru.rbt.security.entity.access.UserMenuItem;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.access.UserMenuItemWrapper;
import ru.rbt.shared.access.UserMenuWrapper;
import ru.rbt.shared.enums.UserMenuCode;
import ru.rbt.shared.enums.UserMenuType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
@Stateless
public class UserMenuRepository extends AbstractBaseEntityRepository<UserMenuItem, Integer> {

    @Inject
    private TextResourceController textResourceController;

    public UserMenuWrapper getUserMenu(AppUser user) {
        try {
            List<DataRecord> allLeafs = select(textResourceController
                    .getContent("ru/rbt/barsgl/ejb/repository/access/all_menu_items.sql"), user.getId());
            if (allLeafs.isEmpty()) {
                return UserMenuWrapper.emptyWrapper();
            }
            // получить все узлы
            List<DataRecord> allNodes = select("SELECT * FROM GL_AU_MENU ORDER BY ID_MENU, PARENT_ID");
            UserMenuWrapper menu = new UserMenuWrapper(allNodes.stream()
                    .filter(rec -> null == rec.getInteger("PARENT_ID"))
                    .map(rec -> new UserMenuItemWrapper(rec.getInteger("ID_MENU"), rec.getString("MENU_NAME")
                            , UserMenuCode.valueOf(rec.getString("MENU_CODE")), UserMenuType.valueOf(rec.getString("MENU_TYPE")))).collect(Collectors.toList()));

            // построить иерархию узлов
            fillChildren(menu.getRootElements(), allNodes);

            // удадяем узлы без действий
            removeRedundant(menu, allLeafs.stream().map(r -> r.getInteger("ID_MENU")).collect(Collectors.toList()));
            return menu;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fillChildren(List<UserMenuItemWrapper> wrappers, List<DataRecord> allNodes) {
        for (UserMenuItemWrapper menuItem : wrappers) {
           List<UserMenuItemWrapper> children = allNodes.stream()
                    .filter(i -> menuItem.getMenuId().equals(i.getInteger("PARENT_ID")) && null != UserMenuCode.parse(i.getString("MENU_CODE")))
                    .map(rec -> new UserMenuItemWrapper(rec.getInteger("ID_MENU"), rec.getString("MENU_NAME")
                            , UserMenuCode.parse(rec.getString("MENU_CODE"))
                            , UserMenuType.valueOf(rec.getString("MENU_TYPE")))).collect(Collectors.toList());
            if (!children.isEmpty()) {
                menuItem.setChildren(children);
                fillChildren(children, allNodes);
            }
        }
    }

    private void removeRedundant(UserMenuWrapper menu, List<Integer> menuIds) {
        walk(menu.getRootElements(), menuIds);
    }

    private void walk(List<UserMenuItemWrapper> nodes, List<Integer> menuIds) {
        Iterator<UserMenuItemWrapper> it = nodes.iterator();
        while (it.hasNext()) {
            UserMenuItemWrapper node = it.next();
            if (!isHasActions(node, menuIds)) {
                it.remove();
            } else {
                if (!node.getChildren().isEmpty()) {
                    walk(node.getChildren(), menuIds);
                }
            }
        }
    }

    private boolean isHasActions(UserMenuItemWrapper node, List<Integer> menuIds) {
        List<UserMenuItemWrapper> allNodes = new ArrayList<>();
        allNodes.add(node);
        fillAllItems(allNodes, node);
        return allNodes.stream().filter(n -> menuIds.contains(n.getMenuId())).findAny().isPresent();
    }

    private void fillAllItems(List<UserMenuItemWrapper> items, UserMenuItemWrapper startNode) {
        if (!startNode.getChildren().isEmpty()) {
            items.addAll(startNode.getChildren());
            for (UserMenuItemWrapper child : startNode.getChildren()) {
                fillAllItems(items, child);
            }
        }
    }
}
