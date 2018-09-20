declare
    a_bsaacid char(20) := ?;
    a_new_dto date := ?;
    l_old_dto date;
    l_cnt_accs number;
    l_msg varchar2(1024);

    type t_old_dt_tab is table of gl_acc.dto%type;
    l_old_dt_tab t_old_dt_tab;

    l_res date;

begin
    update (select dto, (select dto from dual) old_dto from gl_acc where bsaacid = a_bsaacid and dto > a_new_dto)
       set dto = a_new_dto returning old_dto bulk collect into l_old_dt_tab;
    l_cnt_accs := sql%rowcount;

    if (l_cnt_accs > 1) then
        l_msg := pkg_util.msg_format('Too many ACCOUNTS "%s" ("%s") with DTO > "%s"', ARR_STRING(a_bsaacid, to_char(l_cnt_accs), to_char(a_new_dto, 'yyyy-mm-dd')));
        pkg_sys_utl.log_audit_warn('Account', l_msg);
        raise_application_error(-20111, l_msg);
    elsif (l_cnt_accs = 1) then
        l_res := l_old_dt_tab(l_old_dt_tab.first);
    else
        l_res := NULL;
    end if;

    ? := l_res;
end;
