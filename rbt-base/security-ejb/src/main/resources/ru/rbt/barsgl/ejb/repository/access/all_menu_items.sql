-- все доступные пункты меню
select distinct m.*
  from gl_au_usrrl r, gl_au_actrl a, gl_au_menuact ma, gl_au_menu m
 where r.id_user = ? and r.id_role = a.id_role
   and a.id_act = ma.id_act and ma.id_menu = m.id_menu