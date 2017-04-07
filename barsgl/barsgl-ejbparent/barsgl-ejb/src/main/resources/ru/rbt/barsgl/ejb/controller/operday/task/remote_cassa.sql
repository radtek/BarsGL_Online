insert into glvd_bal4 (DAT
                      , ACID
                      , BSAACID
                      , GLACID
                      , OBAL
                      , DTRN
                      , CTRN
                      , UNLOAD_DAT
                      , OBALRUR
                      , DTRNRUR
                      , CTRNRUR
                      , BALOUT
                      , BALRUROUT
                     )
select  v.dat
        , ac.acid
        , ac.bsaacid
        , ac.id glacid
        , case
            when bb.dat = v.dat then bb.obac
            when bb.dat < v.dat then bb.obac + bb.dtac + bb.ctac
        end obal
        , case
            when bb.dat = v.dat then abs(bb.dtac)
            when bb.dat < v.dat then 0
        end dtrn
        , case
            when bb.dat = v.dat then abs(bb.ctac)
            when bb.dat < v.dat then 0
        end ctrn
        , v.dat unload_dat
        , case
            when bb.dat = v.dat then bb.obbc
            when bb.dat < v.dat then bb.obbc + bb.dtbc + bb.ctbc
        end obalrur
        , case
            when bb.dat = v.dat then abs(bb.dtbc)
            when bb.dat < v.dat then 0
        end dtrnrur
        , case
            when bb.dat = v.dat then abs(bb.ctbc)
            when bb.dat < v.dat then 0
        end ctrnrur
        , bb.obac + bb.dtac + bb.ctac balout
        , bb.obbc + bb.dtbc + bb.ctbc baloutrur
  from gl_acc ac, baltur bb, (select date('$1') dat from DUAL) v
 where ac.acc2 = '20208' and dtc is null
   and ac.bsaacid = bb.bsaacid and bb.acid = ac.acid
   and v.dat between bb.dat and bb.datto