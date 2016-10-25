import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;

/**
 * Created by demo01 on 10/18/2016.
 */
public class ProjectedPrice extends AbstractJob {

    String instrumentID;
    private double minEdge = 5;

    public void install (IJobSetup setup){
        setup.addVariable("Instrument", "Instrument to use", "instrument", "");
        setup.addVariable("Min Edge", "Min Edge Threshold", "double", "5.0");
    }

    public void begin (IContainer container){
        super.begin(container);
        instrumentID= instruments().getInstrumentId(container.getVariable("Instrument"));
        minEdge = getDoubleVar("Min Edge");
        container.subscribeToMarketBidAskMessages();
            }


}

