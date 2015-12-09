package org.fuse.usecase.service;

import org.globex.Account;
import org.globex.CorporateAccount;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface CustomerService {

    @WebMethod CorporateAccount updateAccount(Account account);

}
