import com.optionscity.freeway.api.messages.Signal;

/**
 * Created by demo01 on 10/13/2016.
 */
public class FairValueSignal extends Signal {

    public String instrumentID;
    public double price;

    public FairValueSignal (String instrumentID, double price) {
        this.instrumentID=instrumentID;
        this.price=price;
    }

}
