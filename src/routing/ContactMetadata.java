package routing;

import java.util.*;

import core.DTNHost;
import core.SimClock;

public class ContactMetadata
{
    private String hostName;
    private int contactFrequency;
    private double contactDuration;
    private double startEncounterTime;
    private double endEncounterTime;
  //boolean isIndirectRelation = false; if isIndirectRelation = true add host to list
    
    public ContactMetadata()
    {
        super();
        // TODO Auto-generated constructor stub
    }
    
    

    public String getHostName()
    {
        return hostName;
    }



    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }



    public int getContactFrequency()
    {
        return contactFrequency;
    }

    public void setContactFrequency(int contactFrequency)
    {
        this.contactFrequency = contactFrequency;
    }


    public double getContactDuration()
    {
        return contactDuration;
    }

    public void setContactDuration(double averageContactDuration)
    {
        this.contactDuration = averageContactDuration;
    }

    public double getStartEncounterTime()
    {
        return startEncounterTime;
    }

    public void setStartEncounterTime(double startEncounterTime)
    {
        this.startEncounterTime = startEncounterTime;
    }

    public double getEndEncounterTime()
    {
        return endEncounterTime;
    }

    public void setEndEncounterTime(double endEncounterTime)
    {
        this.endEncounterTime = endEncounterTime;
    }
    
    public void update(DTNHost host) {
        setHostName(host.toString());
        setContactFrequency(1);
        setStartEncounterTime(SimClock.getTime());   
        //find a way to set duration
    }



    @Override
    public String toString()
    {
        return "ContactMetadata [hostName=" + hostName + ", contactFrequency=" + contactFrequency + ", contactDuration=" + contactDuration + ", startEncounterTime=" + startEncounterTime
                + ", endEncounterTime=" + endEncounterTime + "]";
    }

    

    

}
