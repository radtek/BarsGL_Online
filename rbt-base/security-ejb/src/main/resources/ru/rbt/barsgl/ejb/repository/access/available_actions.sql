select distinct a.act_code
  from gl_au_usrrl ur, gl_au_actrl ar, gl_au_act a
 where ur.id_role = ar.id_role and ar.id_act = a.id_act
   and ur.id_user = ?