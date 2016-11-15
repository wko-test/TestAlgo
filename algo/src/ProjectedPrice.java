import com.optionscity.freeway.api.*;
import com.optionscity.freeway.api.messages.MarketBidAskMessage;
import com.optionscity.freeway.api.messages.OrderMessage;
import com.optionscity.freeway.api.messages.TradeMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by demo01 on 10/18/2016.
 */

public class ProjectedPrice extends AbstractJob {

    //TODO Make P&L Tracker

    private final String INITIAL_ORDER_LABEL = "new order";
    private final String PROFIT_TAKING_ORDER_LABEL = "profit taking order";
    private final String SCRATCH_ORDER_LABEL = "scratch order";

    String instrumentID;
    private double minEdge = 5;

    //private Map<String, Double> wamMap = new HashMap<>();
    private Set<Long> initialOrderSet;
    private Set<Long> profitTakingOrderSet;
    private Set<Long> scratchingOrderSet;

    private double wam;
    private double minProfitEdge;
    private int orderQty;
    private IGrid orderGrid;
    private IGrid pnlGrid;
    private double PnL=0.0;





    public void install (IJobSetup setup){
        setup.addVariable("Instrument", "Instrument to use", "instrument", "");
        setup.addVariable("Min Edge", "Min Edge Threshold", "double", "5.0");
        setup.addVariable("Order Qty", "Order Quantity", "int", "0");
            }

    public void onMarketBidAsk(MarketBidAskMessage message) {
        if (message.instrumentId.equals(instrumentID)) {
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
            if (trade.side.equals(Order.Side.SELL)) {
                OrderRequest profitOrder = new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, instrumentID, (trade.price - ticksize), trade.quantity);
                profitOrder.label = PROFIT_TAKING_ORDER_LABEL;
                profitOrderID = orders().submit(profitOrder);
            } else {
                OrderRequest profitOrder = new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, instrumentID, (trade.price + ticksize), trade.quantity);
                profitOrder.label = PROFIT_TAKING_ORDER_LABEL;
                profitOrderID = orders().submit(profitOrder);
            }
            profitTakingOrderSet.add(profitOrderID);
        } else if (profitTakingOrderSet.contains(message.orderQuoteId)) {
            double minTickSize = instruments().getInstrumentDetails(instrumentID).minimumPriceIncrement;
            PnL += minTickSize;
            pnlGrid.set("", "PnL", PnL);
        }
    }

    public void onOrder(OrderMessage message) {
        Order order=orders().getOrder(message.orderId);
        if (INITIAL_ORDER_LABEL.equals(order.label) || PROFIT_TAKING_ORDER_LABEL.equals(order.label)){
            orderGrid.set(""+order.orderId, "status", order.status.toString());
            if (!Order.Status.BOOKED.equals(order.status)){
                orderGrid.remove(""+order.orderId);
            }
        }
    }

    public void begin (IContainer container){
        super.begin(container);
        container.addGrid("Orders Grid", new String[]{"status"});
        container.addGrid("PnL Grid", new String[]{"PnL"});
        pnlGrid = container.getGrid("PnL Grid");
        orderGrid = container.getGrid("Orders Grid");
        orderGrid.clear();
        instrumentID= instruments().getInstrumentId(container.getVariable("Instrument"));
        minEdge = getDoubleVar("Min Edge");
        orderQty = getIntVar("Order Qty");
        container.subscribeToMarketBidAskMessages();
        container.subscribeToTradeMessages();
        container.subscribeToOrderMessages();
        initialOrderSet = new HashSet<>();
        profitTakingOrderSet = new HashSet<>();
        scratchingOrderSet = new HashSet<>();

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
        //TODO check edge conversion (dec to 32nd) and make sure it is consistent throughout
        if ((wam - topOfBook.bid) >= minEdge) {
            log ("attmepting to place buy order");
            if (!liveOrdersAtThisPrice(instrumentId, Order.Side.BUY, topOfBook.bid, INITIAL_ORDER_LABEL)) {
                OrderRequest initialOrder = new OrderRequest(Order.Type.LIMIT, Order.Side.BUY, instrumentId, topOfBook.bid, orderQty);
                initialOrder.label = INITIAL_ORDER_LABEL;
                long orderID = orders().submit(initialOrder);
                initialOrderSet.add(orderID);
                log ("Placing buy order " + orderID);
            }
        }
        if ((topOfBook.ask - wam)>= minEdge) {
            log ("attempting to place sell order");
            if (!liveOrdersAtThisPrice(instrumentId, Order.Side.SELL, topOfBook.ask, INITIAL_ORDER_LABEL)) {
                OrderRequest initialOrder = new OrderRequest(Order.Type.LIMIT, Order.Side.SELL, instrumentId, topOfBook.ask, orderQty);
                initialOrder.label = INITIAL_ORDER_LABEL;
                long orderID = orders().submit(initialOrder);
                initialOrderSet.add(orderID);
                log ("Placing sell order " + orderID);
            }
        }
    }

    private void cancelInitialOrdersIfViolatesEdge(String instrumentId){
            for (Long orderId: initialOrderSet){
                Order order = orders().getOrder(orderId);
                boolean isBidLessThanMinEdge = (order.instrumentId.equals(instrumentId)) && (order.side.equals(Order.Side.BUY)) && (wam - order.bookedPrice < minEdge);
                boolean isOfferLessThanMinEdge = (order.instrumentId.equals(instrumentId)) && (order.side.equals(Order.Side.SELL)) && (order.bookedPrice - wam < minEdge);
                if (isBidLessThanMinEdge || isOfferLessThanMinEdge){
                    log("canceling order " + order);
                    orders().cancel(order.orderId);
                }
            }
    }

    private void cancelProfitTakingOrdersIfViolatesEdge (String instrumentID) {
            for (Long orderId: profitTakingOrderSet){
                Order order = orders().getOrder(orderId);
                boolean isBidScratchLessThanMinProfitEdge = (order.instrumentId.equals(instrumentID)) && (order.side.equals(Order.Side.BUY)) && (wam - order.bookedPrice > minProfitEdge);
                boolean isOfferScratchLessThanMinProfitEdge = (order.instrumentId.equals(instrumentID)) && (order.side.equals(Order.Side.SELL)) && (order.bookedPrice - wam > minProfitEdge);
                if (isBidScratchLessThanMinProfitEdge){
                    double minTickSize = instruments().getInstrumentDetails(instrumentID).minimumPriceIncrement;
                    log("scratching order " + order);

                    // Remove from proft taking orders and add to scratching orders
                    scratchingOrderSet.add(orderId);

                    orders().modify(order.orderId, order.bookedQuantity, order.bookedPrice + minTickSize);
            } else if (isOfferScratchLessThanMinProfitEdge) {
                    double minTickSize = instruments().getInstrumentDetails(instrumentID).minimumPriceIncrement;
                    orders().modify(order.orderId, order.bookedQuantity, order.bookedPrice - minTickSize);
            }
        }
        profitTakingOrderSet.removeAll(scratchingOrderSet);
    }

    /**
     * Returns true if we have a booked order at this price for this instrument with the designated side
     * @param instrumentID
     * @param orderSide
     * @param price
     * @return
     */
    private boolean liveOrdersAtThisPrice(String instrumentID, Order.Side orderSide, double price, String label) {
        for (Order order: orders().snapshot()){
            debug ("instrumentID is " + order.instrumentId);
            debug ("booked price is " + order.bookedPrice);
            debug ("side is " + order.side);
            debug ("label is " + order.label);
            boolean orderIsLive = (Order.Status.BOOKED.equals(order.status)) || (Order.Status.NEW.equals(order.status)) || (Order.Status.PARTIAL.equals(order.status));
            boolean ordersMatch = instrumentID.equals(order.instrumentId) && (order.bookedPrice==price) && (orderSide.equals(order.side)) && (label.equals(order.label));
            if (ordersMatch && orderIsLive){
                debug("found order " + order);
                return true;
            }
        }
        return false;
    }



}

