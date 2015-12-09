package org.fuse.usecase.service;

import org.globex.Account;
import org.globex.CorporateAccount;

import javax.jws.WebService;

@WebService(endpointInterface = "org.fuse.usecase.service.CustomerService")
public class CustomerServiceImpl implements CustomerService {

    @Override public CorporateAccount updateAccount(Account account) {
        CorporateAccount corporateAccount = new CorporateAccount();
        corporateAccount.setId(999);
        corporateAccount.setSalesContact("James HetField");
        return corporateAccount;
    }
}
