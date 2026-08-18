# Trading Assistant IA — Backend (TAI-8 en adelante)

Este es el backend real del proyecto (no un spike descartable). Arranca con la integración de Alpaca (TAI-8): datos de mercado (acciones + cripto) y trading manual.

## Configuración

```bash
export ALPACA_API_KEY="tu_api_key_id"
export ALPACA_SECRET_KEY="tu_secret_key"
```

Por default:
- `ALPACA_DATA_BASE_URL` → `https://data.alpaca.markets` (datos de mercado)
- `ALPACA_TRADING_BASE_URL` → `https://paper-api.alpaca.markets` (cuenta **paper**, no real — cambiar a `https://api.alpaca.markets` es una decisión consciente aparte)

## Cómo correrlo

```bash
mvn spring-boot:run
```

Esta app sí levanta servidor web (puerto 8080 por default) — a diferencia del spike de latencia, acá el objetivo es poder probar los endpoints mientras no existe el frontend.

## Endpoints disponibles

**Datos de mercado (solo lectura):**
```
GET /market/stocks/quote/{symbol}              ej: /market/stocks/quote/AAPL
GET /market/stocks/quotes?symbols=AAPL,MSFT    (una request por símbolo internamente, ver nota abajo)
GET /market/stocks/bars/{symbol}?limit=20
GET /market/crypto/quote/{symbol}               ej: /market/crypto/quote/BTC%2FUSD (la "/" va URL-encoded)
GET /market/crypto/bars/{symbol}?limit=20
```

**Trading (cuenta paper por default):**
```
GET  /trading/account
GET  /trading/positions
POST /trading/order?symbol=AAPL&qty=1&side=buy   (MANUAL — ver advertencia abajo)
```

## ⚠️ Importante: el endpoint de orden es manual, no automático

`POST /trading/order` existe para que **vos** lo dispares (con curl, Postman, o más adelante desde el frontend/Telegram) después de revisar un setup. Ningún proceso de este backend lo llama solo — la decisión del Discovery fue "sin ejecución automática de órdenes" hasta confiar en el sistema (ver `07-discovery-personal.md`, sección "Fuera del MVP").

## Decisión de diseño: una request por símbolo, no multi-símbolo

Alpaca tiene un endpoint para pedir varios símbolos en una sola request, pero hay reportes de la comunidad de que a veces devuelve datos de un solo símbolo aunque pidas varios (bug no siempre reproducible). Por eso `getLatestStockQuotes`/`getLatestCryptoQuotes` hacen **una request HTTP por símbolo** — más lento, pero confiable. Con el universo definido (~160 símbolos) y un ciclo de escaneo cada 2-3 minutos (ver TAI-7 en el Discovery), esto entra cómodo en el límite de 200 requests/minuto de Alpaca.

Si en algún momento se quiere optimizar, valdría la pena probar el endpoint multi-símbolo en código real y confirmar si el bug reportado se reproduce o no — quedó anotado como posible mejora futura, no bloqueante.

## Qué sigue

- **TAI-9**: definir el universo dinámico de activos (top 100-150 acciones + top 30 cripto) y usarlo para llamar a `getLatestStockQuotes`/`getLatestCryptoQuotes` en un ciclo programado.
- **TAI-10**: algoritmo de detección de setups sobre las barras que ya trae `getStockBars`/`getCryptoBars`.
- **TAI-11**: Spring AI para la explicación en lenguaje simple (reemplaza el `LlmClient` manual del spike).
