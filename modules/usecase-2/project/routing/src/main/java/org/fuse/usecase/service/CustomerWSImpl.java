package org.fuse.usecase.service;

import org.globex.Account;
import org.globex.CorporateAccount;

import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(endpointInterface = "org.fuse.usecase.service.CustomerWS")
@XmlSeeAlso({org.globex.CorporateAccount.class})
public class CustomerWSImpl implements CustomerWS {

    @Override public CorporateAccount updateAccount(Account account) {
        CorporateAccount corporateAccount = new CorporateAccount();
        corporateAccount.setId(999);
        corporateAccount.setSalesContact("James HetField");
        return corporateAccount;
    }
}
