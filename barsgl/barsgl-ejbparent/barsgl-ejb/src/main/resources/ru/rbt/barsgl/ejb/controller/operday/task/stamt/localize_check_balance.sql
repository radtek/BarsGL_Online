select * from (
  select /*+ parallel (b 4)*/c.dat, b.bsaacid, b.acid, count(1) cnt
    from baltur b, cal c
   where c.dat between b.dat and b.datto and c.ccy = 'RUR' and c.hol <> 'X'
     and c.dat >= ? and c.dat <= ? and b.dat >= ?
    group by c.dat, b.bsaacid, b.acid
    having count(1) > 1
) where rownum <= 10