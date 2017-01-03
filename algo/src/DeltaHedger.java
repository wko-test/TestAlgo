import com.optionscity.freeway.api.*;
import com.sun.org.apache.xerces.internal.impl.dv.xs.DoubleDV;
import com.sun.org.apache.xpath.internal.operations.String;

/**
 * Created by demo01 on 1/3/2017.
 */
public class DeltaHedger extends AbstractJob {

    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable()

        public void begin (IContainer container){
            super.begin(container);
            container.subscribeToTheoMessages();
            container.subscribeToTradeMessages();
            instrumentIDs = instruments().getInstrumentIds(container.getVariable("Instruments"));
        }


    }
        private void updateDelta( String instrumentID ){
            double initDelta;
            double hedgeDelta;
            Greeks(trades().getTrade().delta);

        }
}
