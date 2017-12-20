package ru.rbt.barsgl.ejb.repository.customer;

import ru.rbt.barsgl.ejb.entity.cust.CustDNMapped;
import ru.rbt.barsgl.ejb.entity.cust.Customer;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by er18837 on 19.12.2017.
 */
public class CustomerRepository extends AbstractBaseEntityRepository<Customer, String> {
    public Customer createCustomer(CustDNMapped mappedParam) {
        Customer customer = new Customer(mappedParam.getCustNo());
        customer.setBranch(mappedParam.getBranch());
        customer.setCbType(mappedParam.getCbType());
        customer.setClientType(mappedParam.getClientType());
        customer.setResident(mappedParam.getResident());
        customer.setNameEng(mappedParam.getNameEng());
        customer.setShortNameEng(mappedParam.getShortNameEng());
        customer.setNameRus(mappedParam.getNameRus());

        return save(customer);
    }

    public int updateCustomer(Customer customer, CustDNMapped mappedParam) {
        return executeUpdate("update Customer c set c.branch = ?1, c.resident = ?2, c.cbType = ?3 where c.id = ?4",
                    mappedParam.getBranch(), mappedParam.getResident(), mappedParam.getCbType(), customer.getId());
    }

}
