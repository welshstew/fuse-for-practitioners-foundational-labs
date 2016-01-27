package org.fuse.usecase.aggregate;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.cxf.message.MessageContentsList;
import org.globex.Account;
import org.globex.CorporateAccount;

/**
 * Created by swinchester on 27/01/2016.
 */
public class UseCase2AggregationStrategy implements AggregationStrategy {


//    From the result collected by the 2 services called, you will aggregate
// the content within the multicast processor in order to reconcile the information received from the
// CorporateAccount POJO with the Account POJO.
// This new Account POJO will correspond to the aggregated result that you will next send to another
// Apache Camel route exposed by the direct:insertDB endpoint.
//
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        if (oldExchange == null) {

            return newExchange;
        }

        //so, we transfer data from the corporate account object into the normal account object
        Account normalAccount = null;
        CorporateAccount corporateAccount = null;

        if(oldExchange.getIn().getBody() instanceof  Account)
            normalAccount = (Account) oldExchange.getIn().getBody();

        if(oldExchange.getIn().getBody() instanceof MessageContentsList){
            MessageContentsList mcl = (MessageContentsList) oldExchange.getIn().getBody();
            corporateAccount = (CorporateAccount) mcl.get(0);
        }


        if(newExchange.getIn().getBody() instanceof  Account)
            normalAccount = (Account) newExchange.getIn().getBody();

        if(newExchange.getIn().getBody() instanceof MessageContentsList){
            MessageContentsList mcl = (MessageContentsList) newExchange.getIn().getBody();
            corporateAccount = (CorporateAccount) mcl.get(0);
        }


        //transfer details to the normal account

        normalAccount.setClientId(corporateAccount.getId());
        normalAccount.setSalesRepresentative(corporateAccount.getSalesContact());



        newExchange.getIn().setBody(normalAccount);


        return newExchange;
    }


}
