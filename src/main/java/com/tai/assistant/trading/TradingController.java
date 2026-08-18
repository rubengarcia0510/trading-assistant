package com.tai.assistant.trading;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de trading. GET son de solo lectura; el POST de orden es MANUAL —
 * pensado para que lo dispares vos mismo (curl, Postman, o más adelante el frontend/Telegram),
 * nunca desde un proceso automático. Ver nota en AlpacaTradingClient.
 */
@RestController
@RequestMapping("/trading")
public class TradingController {

    private final AlpacaTradingClient client;

    public TradingController(AlpacaTradingClient client) {
        this.client = client;
    }

    @GetMapping("/account")
    public Account account() {
        return client.getAccount();
    }

    @GetMapping("/positions")
    public List<Position> positions() {
        return client.getOpenPositions();
    }

    /** Ejemplo manual: POST /trading/order?symbol=AAPL&qty=1&side=buy */
    @PostMapping("/order")
    public String placeOrder(@RequestParam String symbol, @RequestParam double qty, @RequestParam String side) {
        return client.placeMarketOrder(symbol, qty, side);
    }
}
