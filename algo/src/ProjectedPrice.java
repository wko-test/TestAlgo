import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.MarketBidAskMessage;
import com.sun.javafx.collections.MappingChange;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by demo01 on 10/18/2016.
 */
public class ProjectedPrice extends AbstractJob {

    String instrumentID;
    private double minEdge = 5;
    private Map<String, Double> wamMap = new HashMap<>();
    private int orderQty;


    public void install (IJobSetup setup){
        setup.addVariable("Instruments", "Instruments to use", "instruments", "");
        setup.addVariable("Min Edge", "Min Edge Threshold", "double", "5.0");
        setup.addVariable("Order Qty", "Order Quantity", "int", "0");
            }

    public void onMarketBidAsk(MarketBidAskMessage message) {
        if (message.instrumentId == instrumentID) {
            log("received market bid/ask for" + message.instrumentId);
            updateWam(message.instrumentId);
            cancelOrdersIfViolatesEdge(message.instrumentId);
            placeOrders(message.instrumentId);
        }
    }

    public void begin (IContainer container){
        super.begin(container);
        instrumentID= instruments().getInstrumentId(container.getVariable("Instrument"));
        minEdge = getDoubleVar("Min Edge");
        orderQty = getIntVar("Order Qty");
        container.subscribeToMarketBidAskMessages();
        updateWam(instrumentID);
        placeOrders(instrumentID);

    }

    private void updateWam(String instrumentId) {
        Prices topOfBook = instruments().getTopOfBook(instrumentId);
        double bidPrice = topOfBook.bid;
        double askPrice = topOfBook.ask;
        double bidQty = topOfBook.bid_size;
        double askQty = topOfBook.ask_size;

        wam = ((bidPrice * bidQty) + (askPrice * askQty)) / (bidQty + askQty);
        debug("WAM is " + wam);

    }

    private void placeOrders(String instrumentId) {
        Prices topOfBook = instruments().getTopOfBook(instrumentID);
        if ((wam - topOfBook.bid) >= minEdge) {
            log ("Placing buy order");
            if (!orderAtThisPrice(instrumentId, Order.Side.BUY, topOfBook.bid)) {
                orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, instrumentId, topOfBook.bid, orderQty));
            }
        }
        if ((topOfBook.ask - wam)>= minEdge) {
            log ("Placing sell order");
            if (!orderAtThisPrice(instrumentId, Order.Side.SELL, topOfBook.ask)) {
                orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, instrumentId, topOfBook.ask, orderQty));
            }
        }
    }

    private void cancelOrdersIfViolatesEdge(String instrumentId){
            for (Order order: orders().snapshot()){
                boolean isBidLessThanMinEdge = (order.instrumentId == instrumentId) && (order.side == Order.Side.BUY) && (wam - order.bookedPrice < minEdge);
                boolean isOfferLessThanMinEdge = (order.instrumentId == instrumentId) && (order.side == Order.Side.SELL) && (order.bookedPrice - wam < minEdge);
                if (isBidLessThanMinEdge || isOfferLessThanMinEdge){
                    log("canceling order " + order);
                    orders().cancel(order.orderId);
                }


            }
    }

    /**
     * Returns true if we have a booked order at this price for this instrument with the designated side
     * @param instrumentID
     * @param orderSide
     * @param price
     * @return
     */
    private boolean orderAtThisPrice(String instrumentID, Order.Side orderSide, double price) {
        for (Order order: orders().snapshot()){
            if (order.instrumentId.equals(instrumentID) && (order.bookedPrice==price) && (order.side==orderSide))
                return true;
        }
        return false;
    }



}

