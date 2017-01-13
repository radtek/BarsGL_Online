-- интервал дат выбираем из календаря
select c.dat
   from cal c
  where c.dat between ? and ?
    and c.ccy = 'RUR' and c.hol <> 'X'
 order by dat