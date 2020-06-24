package routing;

import core.DTNHost;
import core.SimClock;

public class StorageMetadata
{
    private long bufferSize;
    private double bufferOccupancy;
    private double lastEncounterTime;
 
    
    public StorageMetadata()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    public long getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    public double getBufferOccupancy()
    {
        return bufferOccupancy;
    }

    public void setBufferOccupancy(double bufferOccupancy)
    {
        this.bufferOccupancy = bufferOccupancy;
    }

    
    public double getLastEncounterTime()
    {
        return lastEncounterTime;
    }

    public void setLastEncounterTime(double lastEncounterTime)
    {
        this.lastEncounterTime = lastEncounterTime;
    }

    
    
    public void update(DTNHost host) {
        setBufferSize(host.getRouter().getBufferSize());
        setBufferOccupancy(host.getBufferOccupancy());
        setLastEncounterTime(SimClock.getTime());
    }

    @Override
    public String toString()
    {
        return "StorageMetadata [bufferSize=" + bufferSize + ", bufferOccupancy=" + bufferOccupancy + ", lastEncounterTime=" + lastEncounterTime + "]";
    }
    
    
    
    
}
