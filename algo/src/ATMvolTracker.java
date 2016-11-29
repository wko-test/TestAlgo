import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.TheoMessage;

/**
 * Created by demo01 on 11/17/2016.
 */
public class ATMvolTracker extends AbstractJob {

    String instrumentID;
    double vol;
    private IGrid atmGrid;


    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Instrument", "Instrument to chart vols", "instrument", "");

    }
    public void begin(IContainer container) {
        super.begin(container);
        container.subscribeToTheoMessages();
        instrumentID = instruments().getInstrumentId(container.getVariable("Instrument"));
        updateVol(instrumentID);
        container.addGrid("Vol Grid", new String[]{"ATM Vol"});
        atmGrid=container.getGrid("Vol Grid");
    }
    public void onTheo(TheoMessage theoMessage){
        //Update vol here
        if(theoMessage.instrumentId.equals(instrumentID)) {
            updateVol(instrumentID);
        }
    }
    private void updateVol( String instrumentID ){
        Prices topOfBook = instruments().getTopOfBook(instrumentID);
        double bidPrice = topOfBook.bid;
        if (Double.isNaN(bidPrice)) {
            //TODO do something
        }

        double askPrice = topOfBook.ask;
        if (Double.isNaN(askPrice)) {
            //TODO do something
        }

        double undBidPrice = topOfBook.underlying_bid;
        if (Double.isNaN(undBidPrice)) {

        }

        double undAskPrice = topOfBook.underlying_ask;
        if (Double.isNaN(undAskPrice)) {

        }

        double optPrice = (bidPrice + askPrice)/2;
        double undPrice = (undBidPrice + undAskPrice)/2;
        vol=theos().calculateImpliedVolatility(instrumentID, optPrice, undPrice);
        // TODO store vol value in a grid
        atmGrid.set(instrumentID, "ATM Vol", vol);
    }



}
