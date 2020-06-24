package routing;

public class UniqueContactMetadata
{
    
    private double averageContactDuration;
    private int contactFrequency;
    
    public UniqueContactMetadata()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    public UniqueContactMetadata(double averageContactDuration, int contactFrequency)
    {
        super();
        this.averageContactDuration = averageContactDuration;
        this.contactFrequency = contactFrequency;
    }

    public double getAverageContactDuration()
    {
        return averageContactDuration;
    }

    public void setAverageContactDuration(double averageContactDuration)
    {
        this.averageContactDuration = averageContactDuration;
    }

    public int getContactFrequency()
    {
        return contactFrequency;
    }

    public void setContactFrequency(int contactFrequency)
    {
        this.contactFrequency = contactFrequency;
    }

    @Override
    public String toString()
    {
        return "UniqueContactMetadata [averageContactDuration=" + averageContactDuration + ", contactFrequency=" + contactFrequency + "]";
    }
    
    

}
