DECLARE
    L_CNT NUMBER;
BEGIN
    PKG_LOCAL.UPDATE_BVJRNL(L_CNT, A_UNLOAD_STMT => '0');
    ? := L_CNT;
END;