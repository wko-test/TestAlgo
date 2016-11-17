import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;

/**
 * Created by demo01 on 11/17/2016.
 */
public class ATMvolTracker extends AbstractJob {
    @Override
    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Instrument", "Instrument to chart vols", "instrument", "");

    }
    public void begin(IContainer container) {
        super.begin(container);
    }
}
