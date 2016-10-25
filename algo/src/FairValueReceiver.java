import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.helpers.Pricing;

import java.util.SortedSet;

/**
 * Created by demo01 on 10/13/2016.
 */
public class FairValueReceiver extends AbstractJob {

    double minEdge;
    int levels = 3;
    int orderQty;

    public void install(IJobSetup iJobSetup) {
        iJobSetup.addVariable("Min Edge", "Minimum amount of edge", "double", "0.0");
        iJobSetup.addVariable("Order Qty", "Order quantity", "int", "1");
            }

    public void begin (IContainer container) {
        super.begin(container);
        minEdge = getDoubleVar("Min Edge");
        orderQty = getIntVar("Order Qty");
        container.subscribeToSignals();
    }

    public void onSignal (FairValueSignal signal) {
        double firstBid = Pricing.findClosestPriceDown(signal.instrumentID, signal.price - minEdge);
        double firstAsk = Pricing.findClosestPriceUp(signal.instrumentID, signal.price + minEdge);

        SortedSet<Order> orderSnapshot = orders().snapshot();
        for (Order order : orderSnapshot){
            orders().cancel(order.orderId);
        }

        long firstBidOrderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, signal.instrumentID, firstBid, orderQty));
        long firstAskOrderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, signal.instrumentID, firstAsk, orderQty));

        InstrumentDetails instrumentDetails = instruments().getInstrumentDetails(signal.instrumentID);
        double tickSize = instrumentDetails.displayTickSize;

        for (int i=1; i<levels; i++) {
            double nextBid;
            double nextAsk;
            nextBid = firstBid - i*tickSize;
            nextAsk = firstAsk + i*tickSize;

            orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, signal.instrumentID, nextBid, orderQty));
            orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, signal.instrumentID,nextAsk, orderQty));

        }
    }
}
