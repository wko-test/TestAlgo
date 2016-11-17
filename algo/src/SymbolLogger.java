import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.InstrumentDetails;

import java.util.Collection;

/**
 * Created by demo01 on 11/17/2016.
 */
public class SymbolLogger extends AbstractJob {

    private Collection<String> instrumentIDs;


    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Instrument", "Instrument to Log", "instruments", "");

    }

    public void begin(IContainer container){
        super.begin(container);

        instrumentIDs = instruments().getInstrumentIds(container.getVariable("Instrument"));

        for (String tempInstrumendID : instrumentIDs) {
                log(tempInstrumendID);
             String expMonths = instruments().getInstrumentDetails(tempInstrumendID).displayExpiration;

        }
    }
}
