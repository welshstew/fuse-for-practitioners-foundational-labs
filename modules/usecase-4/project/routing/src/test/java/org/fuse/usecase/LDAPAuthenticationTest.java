package org.fuse.usecase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
    @CreateTransport(protocol = "LDAP", port = 1024) })
@ApplyLdifFiles("org/fuse/usecase/activemq.ldif")
public class LDAPAuthenticationTest extends AbstractLdapTestUnit {

    public BrokerService broker;
    public static LdapServer ldapServer;

    @Before
    public void setup() throws Exception {
        broker = BrokerFactory.createBroker("xbean:org/fuse/usecase/activemq-broker.xml");
        broker.start();
        broker.waitUntilStarted();
    }

    @After
    public void shutdown() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }

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
        // performAdminAccountChecks(entry);
        EntryAttribute attrPwd = entry.get("userPassword");
        String ldapPwd = attrPwd.get().getString();
        String pwdHashed = hashSSHAPassword("secret");

        assertEquals(ldapPwd,pwdHashed);
        connection.close();
    }

    @Test
    public void testSucceed() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection conn = factory.createQueueConnection("jdoe", "secret");
        try {
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            conn.start();
            Queue queue = sess.createQueue("ADMIN.FOO");

            MessageProducer producer = sess.createProducer(queue);
            MessageConsumer consumer = sess.createConsumer(queue);

            producer.send(sess.createTextMessage("test"));
            Message msg = consumer.receive(1000);
            assertNotNull(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    String hashSSHAPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(password.getBytes("UTF8"));
        byte[] pwdDigested = digest.digest();
        char[] sshaPassword = Base64.encode(pwdDigested);
        return "{SSHA}" + String.valueOf(sshaPassword);
    }
}