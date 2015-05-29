package org.mdpnp.devices;


import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantQos;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Properties;

public class DomainParticipantFactoryTest {

    @Test
    public void testRegexSplit() throws Exception {
        String a[];

        a=DomainParticipantFactory.split("127.10.0.1 127.10.0.2");
        Assert.assertEquals(a.length, 2);

        a=DomainParticipantFactory.split("127.10.0.1;127.10.0.2");
        Assert.assertEquals(a.length, 2);

        a=DomainParticipantFactory.split("127.10.0.1     127.10.0.2");
        Assert.assertEquals(a.length, 2);

        a=DomainParticipantFactory.split("127.10.0.1;127.10.0.2 127.10.0.3");
        Assert.assertEquals(a.length, 3);

        a=DomainParticipantFactory.split("");
        Assert.assertEquals(a.length, 0);

        a=DomainParticipantFactory.split("   ");
        Assert.assertEquals(a.length, 0);
    }

    private ConfigurableApplicationContext createContext(String discoveryPeers) throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] { "RtConfig.xml" }, false);
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/RtConfig.properties"));
        if(discoveryPeers == null)
            props.remove("dds.discovery.peers");
        else
            props.setProperty("dds.discovery.peers", discoveryPeers);
        ppc.setProperties(props);
        ppc.setOrder(0);

        ctx.addBeanFactoryPostProcessor(ppc);
        ctx.refresh();
        return ctx;
    }

    @Test
    public void testDefaultDiscoverySetting() throws Exception {

        ConfigurableApplicationContext ctx = createContext(null);
        try {
            DomainParticipant dp = ctx.getBean(DomainParticipant.class);
            DomainParticipantQos qos = new DomainParticipantQos();
            dp.get_qos(qos);
            Assert.assertEquals(qos.discovery.initial_peers.size(), 1);
            Assert.assertEquals(qos.discovery.initial_peers.get(0), "udpv4://239.255.0.1");
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.size(), 1);
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.get(0), "udpv4://239.255.0.1");
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testCustomSettingWithMulticast1() throws Exception {

        ConfigurableApplicationContext ctx = createContext("239.255.0.1, 127.0.0.1");
        try {
            DomainParticipant dp = ctx.getBean(DomainParticipant.class);
            DomainParticipantQos qos = new DomainParticipantQos();
            dp.get_qos(qos);
            Assert.assertEquals(qos.discovery.initial_peers.size(), 2);
            Assert.assertEquals(qos.discovery.initial_peers.get(0), "udpv4://239.255.0.1");
            Assert.assertEquals(qos.discovery.initial_peers.get(1), "udpv4://127.0.0.1");
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.size(), 1);
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.get(0), "udpv4://239.255.0.1");
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testCustomSettingWithMulticast2() throws Exception {

        ConfigurableApplicationContext ctx = createContext("127.0.0.1, 239.255.0.1");
        try {
            DomainParticipant dp = ctx.getBean(DomainParticipant.class);
            DomainParticipantQos qos = new DomainParticipantQos();
            dp.get_qos(qos);
            Assert.assertEquals(qos.discovery.initial_peers.size(), 2);
            Assert.assertEquals(qos.discovery.initial_peers.get(0), "udpv4://127.0.0.1");
            Assert.assertEquals(qos.discovery.initial_peers.get(1), "udpv4://239.255.0.1");
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.size(), 1);
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.get(0), "udpv4://239.255.0.1");
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testCustomSettingWithMulticast3() throws Exception {

        ConfigurableApplicationContext ctx = createContext("239.255.0.1, 239.255.1.1");
        try {
            DomainParticipant dp = ctx.getBean(DomainParticipant.class);
            DomainParticipantQos qos = new DomainParticipantQos();
            dp.get_qos(qos);
            Assert.assertEquals(qos.discovery.initial_peers.size(), 2);
            Assert.assertEquals(qos.discovery.initial_peers.get(0), "udpv4://239.255.0.1");
            Assert.assertEquals(qos.discovery.initial_peers.get(1), "udpv4://239.255.1.1");
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.size(), 1);
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.get(0), "udpv4://239.255.0.1");
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testCustomSettingNoMulticast() throws Exception {

        ConfigurableApplicationContext ctx = createContext("127.0.0.1, 127.0.0.2");
        try {
            DomainParticipant dp = ctx.getBean(DomainParticipant.class);
            DomainParticipantQos qos = new DomainParticipantQos();
            dp.get_qos(qos);
            Assert.assertEquals(qos.discovery.initial_peers.size(), 2);
            Assert.assertEquals(qos.discovery.initial_peers.get(0), "udpv4://127.0.0.1");
            Assert.assertEquals(qos.discovery.initial_peers.get(1), "udpv4://127.0.0.2");
            Assert.assertEquals(qos.discovery.multicast_receive_addresses.size(), 0);

        }
        finally {
            ctx.close();
        }
    }

}
