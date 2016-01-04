package org.fuse.usecase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.jndi.JndiUtils;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.util.Hashtable;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
    @CreateTransport(protocol = "LDAP", port = 1024) })
@ApplyLdifFiles("org/fuse/usecase/activemq.ldif")
public class LDAPAuthenticationTest extends AbstractLdapTestUnit {

    public BrokerService broker;
    public static LdapServer ldapServer;

/*    @Before
    public void setup() throws Exception {
        //System.setProperty("ldapPort", String.valueOf(1024));
        //System.setProperty("java.security.auth.login.config","login.config");
        broker = BrokerFactory.createBroker("xbean:org/fuse/usecase/activemq-broker.xml");
        broker.start();
        broker.waitUntilStarted();
    }

    @After
    public void shutdown() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }*/

    /**
     * Checks all attributes of the admin account entry minus the userPassword
     * attribute.
     *
     * @param entry the entries attributes
     */
    protected void performAdminAccountChecks(Entry entry) {
        assertTrue(entry.get("objectClass").contains("top"));
        assertTrue(entry.get("objectClass").contains("person"));
        assertTrue(entry.get("objectClass").contains("organizationalPerson"));
        assertTrue(entry.get("objectClass").contains("inetOrgPerson"));
        assertTrue(entry.get("displayName").contains("Directory Superuser"));
    }

    @Test
    public void testSearchAllAttrs() throws Exception {
        String userDn = "uid=admin,ou=User,ou=ActiveMQ,ou=system";
        LdapConnection connection = new LdapConnection("127.0.0.1", ldapServer.getPort());
        connection.bind(userDn, "secret");

        Entry entry = ((SearchResultEntry) connection.lookup(userDn)).getEntry();
        performAdminAccountChecks(entry);
        connection.close();
    }


    /*        // Try to read an entry in the server
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getPort() );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        LdapContext ctx = new InitialLdapContext( env, null );*/


/*    @Test
    public void testSucceed() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection conn = factory.createQueueConnection("admin", "secret");
        try {
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            conn.start();
            Queue queue = sess.createTemporaryQueue();

            MessageProducer producer = sess.createProducer(queue);
            MessageConsumer consumer = sess.createConsumer(queue);

            producer.send(sess.createTextMessage("test"));
            Message msg = consumer.receive(1000);
            assertNotNull(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }*/
}