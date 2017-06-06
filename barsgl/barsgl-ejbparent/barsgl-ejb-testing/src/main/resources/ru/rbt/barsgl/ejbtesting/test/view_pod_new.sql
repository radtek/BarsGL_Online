declare
    PRAGMA AUTONOMOUS_TRANSACTION;
begin
    execute immediate 'CREATE OR REPLACE VIEW V_GL_OPER_POD as
                       select v1.gloid, v1.pod_type,
                             case when v1.pod_type = ''HARD'' then cast (''2012-12-12'' as date) else cast (null as date) end pod
                        from
                       (
                          select gloid,
                                 case when o.fan = ''Y'' and o.strn = ''N'' then ''CURRENT''
                                      when o.fan = ''N'' and o.strn = ''Y'' then ''LAST''
                                      when o.fan = ''Y'' and o.strn = ''Y'' then ''STANDARD''
                                      when o.fan = ''N'' and o.strn = ''N'' then ''HARD''
                                 end pod_type
                            from GL_OPER o
                        ) v1';
    commit;
end;

