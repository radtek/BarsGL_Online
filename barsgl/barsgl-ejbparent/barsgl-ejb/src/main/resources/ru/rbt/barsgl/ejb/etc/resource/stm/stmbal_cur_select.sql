DECLARE
    L_CNT NUMBER;
BEGIN
    PKG_STMT_BALANCE_FULL.FILL_DATA(a_date => ?, a_cnt => l_cnt);
    ? := L_CNT;
END;