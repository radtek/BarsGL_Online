select b.*
from baltur b, gl_acc acc
where b.bsaacid = acc.bsaacid
 and acc.rev_fl='Y' 
 and (dat = ?
      or dat >= value((select DATE_UPL from gl_rvacul where bsaacid=b.bsaacid),'2029-01-01')
