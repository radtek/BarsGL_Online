declare
    l_handle varchar2(256);
begin
    PKG_SYNC.REQUEST_LOCK(?, ?, l_handle);
    ? := l_handle;
end;