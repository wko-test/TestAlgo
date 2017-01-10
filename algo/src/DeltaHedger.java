import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.TradeMessage;


/**
 * Created by demo01 on 1/3/2017.
 */
public class DeltaHedger extends AbstractJob {


    String instrumentMonth;

    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Instrument Month", "Instrument Month to Hedge", "String", "");

    }

    public void begin (IContainer container){
        super.begin(container);
        container.subscribeToTradeMessages();
        instrumentMonth = getStringVar("Instrument Month");

    }

    public void onTrade (TradeMessage message){
        InstrumentDetails details = instruments().getInstrumentDetails(message.instrumentId);
        if (details.instrumentMonth.equals(instrumentMonth)){
            hedge(message);
        }
    }

    private void hedge (TradeMessage message){}


    private void updateDelta( String instrumentID ){
        double initDelta;
        double hedgeDelta;
        Greeks(trades().getTrade().delta);

    }
}

//Todo: get trade inst, get delta for that inst (From theo service), get traded qty, total trade delta = delta x qty, hedge as much as possible, store remainder to be hedged to a global var;
