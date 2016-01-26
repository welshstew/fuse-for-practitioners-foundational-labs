package org.fuse.usecase.service;

import org.acme.Customer;
import org.globex.Account;
import org.globex.Company;

import java.util.Random;

public class CustomerRestImpl implements CustomerRest {

    private static final String NA_REGION = "NORTH_AMERICA";
    private static final String SA_REGION = "SOUTH_AMERICA";
    private static final String WE_REGION = "WEST_AMERICA";
    private static final String EAST_REGION = "EAST_AMERICA";

    @Override
    public Account enrich(Account account) {
        Company company = account.getCompany();
        String region = company.getGeo();

        if(region.equalsIgnoreCase("na")){
            company.setGeo(NA_REGION);
        }

        if(region.equalsIgnoreCase("sa")){
            company.setGeo(SA_REGION);
        }

        if(region.equalsIgnoreCase("wa")){
            company.setGeo(WE_REGION);
        }

        if(region.equalsIgnoreCase("ea")){
            company.setGeo(EAST_REGION);
        }

        return account;
    }

    @Override
    public String hello() {
        return "hello";
    }
}
