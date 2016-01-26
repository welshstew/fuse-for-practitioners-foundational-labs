

/**
 * Created by swinchester on 26/01/2016.
 */
import groovy.json.JsonOutput
import org.acme.Customer
import org.globex.Account
import org.globex.Company
import org.globex.Contact

def ic = request.body as Customer

def acct = new Account(company: new Company(active: ic.active,
                                            geo: ic.region,
                                            name: ic.companyName),
                        contact: new Contact(city: ic.city,
                                             firstName: ic.firstName,
                                            lastName: ic.lastName,
                                            phone: ic.phone,
                                            state: ic.state,
                                            streetAddr: ic.streetAddr,
                                            zip: ic.zip))
request.body = JsonOutput.toJson(acct)