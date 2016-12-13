import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.TheoMessage;

import java.util.LinkedList;

/**
 * Created by demo01 on 11/17/2016.
 */
public class ATMvolTracker extends AbstractJob {

    String instrumentID;
    double vol;
    private IGrid atmGrid;
    int n = 1;
    LinkedList<Double> volData= new LinkedList<>();
    int volDataSize = 10;

    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Instrument", "Instrument to chart vols", "instrument", "");

    }
    public void begin(IContainer container) {
        super.begin(container);
        container.subscribeToTheoMessages();
        instrumentID = instruments().getInstrumentId(container.getVariable("Instrument"));
        atmGrid = container.addGrid("Vol Grid", new String[]{"ATM Vol", "ATM MA-Live", "ATM MA10p"});
        atmGrid=container.getGrid("Vol Grid");
        atmGrid.clear();
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
        InstrumentDetails details = instruments().getInstrumentDetails(instrumentID);
        String underlyingID = details.underlyingId;
        Prices undTopOfBook = instruments().getTopOfBook(underlyingID);
        double bidPrice = topOfBook.bid;
        double askPrice = topOfBook.ask;
        double optPrice;
        double undBidPrice = undTopOfBook.bid;
        double undAskPrice = undTopOfBook.ask;
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
        log("Underlying ID is "+underlyingID+" , Underlying Bid is "+undTopOfBook.bid+", Underlying Ask is "+undTopOfBook.ask);
        vol=theos().calculateImpliedVolatility(instrumentID, optPrice, undPrice);
        log("Found vol of "+vol+"using Underlying price of " +undPrice+"and Option price of " +optPrice);

        atmGrid.set(instrumentID, "ATM Vol", vol);

        double MAlive;

        if (n==1){
            MAlive = vol;
        }   else {
            MAlive = atmGrid.getDouble(instrumentID, "ATM MA-Live");
            MAlive = ((n-1)*MAlive + vol)/n;
        }
        n++;
        atmGrid.set(instrumentID, "ATM MA-Live", MAlive);
        addVol(vol);
        double MA10p = getAvgVolOverTime();

        atmGrid.set(instrumentID, "ATM MA10p", vol);

    }

    private void addVol (double vol){
        if (volData.size()==volDataSize){
            volData.pop();
        }
        volData.add(vol);
    }

    private double getAvgVolOverTime() {
        if (volData.size()<volDataSize){
            return Double.NaN;
        }

        double sum = 0;
        for (double vol : volData) {
            sum += vol;
        }
        return sum/volDataSize;
    }

}
