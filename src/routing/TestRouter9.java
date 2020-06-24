package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;
import routing.util.RoutingInfo;

public class TestRouter9 extends ActiveRouter
{
    List<ContactMetadata> contactMetrics;
    private Map<DTNHost, StorageMetadata> storageMetrics;
    private Map<DTNHost, UniqueContactMetadata> contactMetadata;
    
   
    public TestRouter9(Settings s) {
        super(s);
    }
   
    protected TestRouter9(TestRouter9 r) {
        super(r);
        initBufferSizes();
    }
   
    private void initBufferSizes() {
        this.contactMetrics = new ArrayList<ContactMetadata>();
        this.storageMetrics = new HashMap<DTNHost, StorageMetadata>();
        this.contactMetadata = new HashMap<DTNHost, UniqueContactMetadata>();
    }
   
    @Override
    public void changedConnection(Connection con) {
        if (con.isUp()) {
       
            DTNHost otherHost = con.getOtherNode(getHost());
            updateMetricsFor(otherHost);
            updateTransitiveMetrics((TestRouter9)otherHost.getRouter());
        }
    }
   
    private void updateMetricsFor(DTNHost host) {
        int counter = 0;
        double sum = 0.0;
        double averageContactDuration = 0.0;
        StorageMetadata sm = new StorageMetadata();
        ContactMetadata cm = new ContactMetadata();
        sm.update(host);
        cm.update(host);
        contactMetrics.add(cm);
        storageMetrics.put(host, sm);   
        
        for(ContactMetadata contact : contactMetrics) {
            if(contact.getHostName()==host.toString()) {
                counter++;
                sum+= contact.getContactDuration();
            }
            if(counter!=0) {
                averageContactDuration= sum/counter;
            }
            UniqueContactMetadata uniqueContact = new UniqueContactMetadata(averageContactDuration, counter);
            contactMetadata.put(host, uniqueContact);  
        }
    }
    
    private void updateTransitiveMetrics(TestRouter9 router) {
        Map<DTNHost, StorageMetadata> othersStorageMetadata =
                router.getStorageMetrics();
        Map<DTNHost, UniqueContactMetadata> othersContactMetadata =
                router.getContactMetrics();

        for (Map.Entry<DTNHost, StorageMetadata> e : othersStorageMetadata.entrySet()) {
            DTNHost h = e.getKey();
            StorageMetadata sm = e.getValue();
            if (h == getHost()) {
                continue; // don't add yourself
            }
            storageMetrics.put(h,sm);

        }
        
        for (Map.Entry<DTNHost, UniqueContactMetadata> e : othersContactMetadata.entrySet()) {
            DTNHost h = e.getKey();
            UniqueContactMetadata cm = e.getValue();
            if (h == getHost()) {
                continue; // don't add yourself
            }
            contactMetadata.put(h,cm);

        }
    }
    
   
    private Map<DTNHost, UniqueContactMetadata> getContactMetrics() {
        return this.contactMetadata;
    }
    
    private Map<DTNHost,StorageMetadata> getStorageMetrics() {
        return this.storageMetrics;
    }
    
    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() ||isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }
        
        tryMetadataMessageExchange();

        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        tryOtherMessages();
    }
     
    private void tryMetadataMessageExchange() {
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            TestRouter9 othRouter = (TestRouter9)other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }
            if (othRouter.hasMessage("metadata")) {
                continue; // do not send metadata message again
            }
        Message metadata = new Message(getHost(),other,"metadata", this.getContactMetrics().size() + this.getStorageMetrics().size());
        getHost().createNewMessage(metadata);
        startTransfer(metadata,con);
        }

    }
    
    
    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
            new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            TestRouter9 othRouter = (TestRouter9)other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }
            
            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                
                    messages.add(new Tuple<Message, Connection>(m,con));
                
            }
        }

        if (messages.size() == 0) {
            return null;
        }

        return tryMessagesForConnected(messages);   // try to send messages
    }
    

    private String showInfoHost(DTNHost h) {
String info = h.toString() + ": " + this.contactMetadata.get(h) + " " + this.contactMetadata.get(h);
return info;
    }

    @Override
    public TestRouter9 replicate() {
        TestRouter9 r = new TestRouter9(this);
        return r;
    }

    @Override
    public RoutingInfo getRoutingInfo() {
    RoutingInfo top = super.getRoutingInfo();
RoutingInfo ri = new RoutingInfo("Metadata Metrics");
System.out.println();
for (DTNHost key: getStorageMetrics().keySet()){
ri.addMoreInfo(new RoutingInfo(String.format("%s ", showInfoHost(key))));
}
top.addMoreInfo(ri);

return top;
    }
}