package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.ActSrc;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.repository.dict.AccType.ActSrcRepository;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.UserProductsWrapper;
import ru.rbt.barsgl.shared.dict.AccTypeSourceWrapper;
import static java.lang.String.format;
import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.audit.entity.AuditRecord.LogCode.ActSrc;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;
/**
 * Created by akichigi on 30.09.16.
 */
public class ActSrcController {
    @EJB
    private AuditController auditController;

    @Inject
    private ActSrcRepository actSrcRepository;

    @Inject
    private SourcesDealsRepository sourcesDealsRepository;

    public RpcRes_Base<AccTypeSourceWrapper> setSrcLinkedToAccType(AccTypeSourceWrapper wrapper){
        try{
              deleteProducts(wrapper.getAcctype());
              for(UserProductsWrapper product: wrapper.getGranted_products()){
                  //if (!isProductExists(wrapper.getAcctype(), product.getCode())){
                      saveActSrc(wrapper.getAcctype(), product.getCode());
                      auditController.info(ActSrc, format("AccType '%s' связан с источником сделки '%s'",
                              wrapper.getAcctype(), product.getCode()));
                 // }
              }

            return new RpcRes_Base<AccTypeSourceWrapper>(wrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(ActSrc, "Ошибка сохранения источников сделки!", null, e);
            return new RpcRes_Base<AccTypeSourceWrapper>(wrapper, true, errMessage);
        }
    }

    private void deleteProducts(String accType){
        List<ActSrc> products = actSrcRepository.select(ActSrc.class, "from ActSrc r where r.acctype = ?1", accType);
        for (ru.rbt.barsgl.ejb.entity.dict.AccType.ActSrc product : products){
            actSrcRepository.remove(product);
            auditController.info(ActSrc, format("AccType '%s' отвязан от источника сделки '%s'",
                    accType, product.getDealsrc()));
        }
        actSrcRepository.flush();
    }

    private boolean isProductExists(String accType, String dealSource){
        return null != actSrcRepository.selectFirst(ActSrc.class, "from ActSrc r where r.acctype = ?1 and r.dealsrc = ?2", accType, dealSource);
    }

    private void saveActSrc(String accType, String dealSource){
        ru.rbt.barsgl.ejb.entity.dict.AccType.ActSrc actSrc = new ActSrc(accType, dealSource);
        actSrcRepository.save(actSrc);
    }

    public RpcRes_Base<AccTypeSourceWrapper> getSrcLinkedToAccType(String accType){
        AccTypeSourceWrapper wrapper = new AccTypeSourceWrapper();
        try{
            wrapper.setGranted_products(products(accType, false));
            wrapper.setProducts(products(accType, true));
            return new RpcRes_Base<AccTypeSourceWrapper>(wrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(ActSrc, "Ошибка получения источников сделки!", null, e);
            return new RpcRes_Base<AccTypeSourceWrapper>(wrapper, true, errMessage);
        }
    }

    private ArrayList<UserProductsWrapper> products(String accType, boolean all){
        ArrayList<UserProductsWrapper> list = new ArrayList<>();

        if (all){
            List<String> pList = actSrcRepository.getActSrcByAct(accType).stream().map(m -> m.getDealsrc()).collect(Collectors.toList());
            List<SourcesDeals> fList = sourcesDealsRepository.getAllObjectsCached().stream().filter(f -> !pList.contains(f.getId()))
                    .collect(Collectors.toList());

            for(SourcesDeals deals: fList){
                UserProductsWrapper wrapper = new UserProductsWrapper();
                wrapper.setCode(deals.getId());
                list.add(wrapper);
            }
        }else {
            for (ActSrc src: actSrcRepository.getActSrcByAct(accType)){
                UserProductsWrapper wrapper = new UserProductsWrapper();
                wrapper.setCode(src.getDealsrc());
                list.add(wrapper);
            }
        }

        return list;
    }
}
