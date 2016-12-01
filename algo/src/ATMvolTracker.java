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
        atmGrid = container.addGrid("Vol Grid", new String[]{"ATM Vol"});
        atmGrid=container.getGrid("Vol Grid");
        updateVol(instrumentID);
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
        double askPrice = topOfBook.ask;
        double optPrice;
        double undBidPrice = topOfBook.underlying_bid;
        double undAskPrice = topOfBook.underlying_ask;
        double undPrice;

        if (Double.isNaN(askPrice)&&(Double.isNaN(bidPrice))) {
            log("Missing bid and ask option data...unable to set vol");
            return;
        } else if (Double.isNaN(askPrice)) {
            log("Ask is null, using Bid for vol calc");
            optPrice = bidPrice;
        } else if (Double.isNaN(bidPrice)) {
            log("Bid is null, using Ask for vol calc");
            optPrice = askPrice;
        } else {
            log("calculating vol");
            optPrice = (bidPrice + askPrice)/2;
        }

        if (Double.isNaN(undAskPrice)&&(Double.isNaN(undBidPrice))) {
            log("Underlying Bid and Ask are null");
            return;
        }   else if (Double.isNaN(undBidPrice)) {
            log("Underlying Bid Price is null, using Ask");
            undPrice = undAskPrice;
        }   else if (Double.isNaN(undAskPrice)) {
            log("Underlying Ask Price is null, using Bid");
            undPrice = undBidPrice;
        }   else {
            log("Calculating Underlying");
            undPrice = (undBidPrice + undAskPrice)/2;
        }
        InstrumentDetails details = instruments().getInstrumentDetails(instrumentID);
        String underlyingID = details.underlyingId;
        Prices undTopOfBook = instruments().getTopOfBook(underlyingID);
                log("Underlying ID is "+underlyingID+" , Underlying Bid is "+undTopOfBook.bid+", Underlying Ask is "+undTopOfBook.ask);
        vol=theos().calculateImpliedVolatility(instrumentID, optPrice, undPrice);
        log("Found vol of "+vol+"using Underlying price of " +undPrice+"and Option price of " +optPrice);

        atmGrid.set(instrumentID, "ATM Vol", vol);
    }



}