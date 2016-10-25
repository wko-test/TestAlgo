import com.optionscity.freeway.api.AbstractJob;
import com.optionscity.freeway.api.IContainer;
import com.optionscity.freeway.api.IJobSetup;
import com.optionscity.freeway.api.Prices;
import com.optionscity.freeway.api.messages.MarketBidAskMessage;

/**
 * Created by demo01 on 10/13/2016.
 */
public class FairValueSender extends AbstractJob {

    private String instrumentID;
    private double lastBid = Double.NaN;
    private double lastAsk = Double.NaN;

    public void install(IJobSetup setup) {
        setup.addVariable("Instrument", "Instrument to send fair values", "instrument", "");
            }

    public void begin(IContainer container) {
        super.begin(container);
        container.subscribeToMarketBidAskMessages();
        instrumentID = instruments().getInstrumentId(container.getVariable("Instrument"));
    }

    public void onMarketBidAsk(MarketBidAskMessage message){
        long lag = System.currentTimeMillis() - message.timestamp;
        if(message.instrumentId == instrumentID) {
            Prices topOfBook = instruments().getTopOfBook(instrumentID);
            boolean pricesHaveChanged = (topOfBook.bid != lastBid || topOfBook.ask != lastAsk);

            if(pricesHaveChanged){
                double fairValue = .5 * (topOfBook.bid + topOfBook.ask);
                container.signal(new FairValueSignal(instrumentID, fairValue));
                lastAsk = topOfBook.ask;
                lastBid = topOfBook.bid;
            }
        }
    }


}
