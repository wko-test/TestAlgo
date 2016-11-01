import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.MarketBidAskMessage;
import com.optionscity.freeway.api.messages.TradeMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by demo01 on 10/18/2016.
 */
public class ProjectedPrice extends AbstractJob {

    String instrumentID;
    private double minEdge = 5;
    //private Map<String, Double> wamMap = new HashMap<>();
private Set<Long> initialOrderSet;
private Set<Long> profitTakingOrderSet;
    private double wam;
    private double minProfitEdge;
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
            cancelInitialOrdersIfViolatesEdge(message.instrumentId);
            placeInitialOrders(message.instrumentId);
        }
    }

    public void onTrade(TradeMessage message) {
        if (initialOrderSet.contains(message.orderQuoteId)) {
            double ticksize = 0;
            Trade trade= trades().getTrade(message.tradeId);
            long profitOrderID;
            if (trade.side== Order.Side.SELL) {
                profitOrderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, instrumentID, (trade.price - ticksize), trade.quantity));
            } else {
                profitOrderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, instrumentID, (trade.price + ticksize), trade.quantity));
            }
            profitTakingOrderSet.contains(profitOrderID);
        }
    }



    public void begin (IContainer container){
        super.begin(container);
        instrumentID= instruments().getInstrumentId(container.getVariable("Instrument"));
        minEdge = getDoubleVar("Min Edge");
        orderQty = getIntVar("Order Qty");
        container.subscribeToMarketBidAskMessages();
        container.subscribeToTradeMessages();
        initialOrderSet = new HashSet<>();
        profitTakingOrderSet = new HashSet<>();
        updateWam(instrumentID);
        placeInitialOrders(instrumentID);

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

    private void placeInitialOrders(String instrumentId) {
        Prices topOfBook = instruments().getTopOfBook(instrumentID);
        if ((wam - topOfBook.bid) >= minEdge) {
            log ("Placing buy order");
            if (!orderAtThisPrice(instrumentId, Order.Side.BUY, topOfBook.bid)) {
                long orderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, instrumentId, topOfBook.bid, orderQty));
                initialOrderSet.add(orderID);
            }
        }
        if ((topOfBook.ask - wam)>= minEdge) {
            log ("Placing sell order");
            if (!orderAtThisPrice(instrumentId, Order.Side.SELL, topOfBook.ask)) {
                long orderID = orders().submit(new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, instrumentId, topOfBook.ask, orderQty));
                initialOrderSet.add(orderID);
            }
        }
    }

    private void cancelInitialOrdersIfViolatesEdge(String instrumentId){
            for (Long orderId: initialOrderSet){
                Order order = orders().getOrder(orderId);
                boolean isBidLessThanMinEdge = (order.instrumentId == instrumentId) && (order.side == Order.Side.BUY) && (wam - order.bookedPrice < minEdge);
                boolean isOfferLessThanMinEdge = (order.instrumentId == instrumentId) && (order.side == Order.Side.SELL) && (order.bookedPrice - wam < minEdge);
                if (isBidLessThanMinEdge || isOfferLessThanMinEdge){
                    log("canceling order " + order);
                    orders().cancel(order.orderId);
                }
            }
    }

    private void cancelProfitTakingOrdersIfViolatesEdge (String instrumentID) {
            for (Long orderId: profitTakingOrderSet){
                Order order = orders().getOrder(orderId);
                boolean isBidScratchLessThanMinProfitEdge = (order.instrumentId == instrumentID) && (order.side == Order.Side.BUY) && (wam - order.bookedPrice > minProfitEdge);
                boolean isOfferScratchLessThanMinProfitEdge = (order.instrumentId == instrumentID) && (order.side == Order.Side.SELL) && (order.bookedPrice - wam > minProfitEdge);
                if (isBidScratchLessThanMinProfitEdge){
                    double minTickSize = 0;
                    log("scratching order " + order);
                    orders().modify(order.orderId, order.bookedQuantity, order.bookedPrice + minTickSize);
            } else if (isOfferScratchLessThanMinProfitEdge) {
                    double minTickSize = 0;
                    log("scratching order " + order);
                    orders().modify(order.orderId, order.bookedQuantity, order.bookedPrice - minTickSize);
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

