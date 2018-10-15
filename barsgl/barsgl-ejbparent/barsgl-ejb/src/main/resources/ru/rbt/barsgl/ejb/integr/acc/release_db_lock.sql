declare
    l_hndl varchar2(256) := ?;
begin
    PKG_SYNC.RELEASE_LOCK(l_hndl);
end;
