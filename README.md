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

- **TAI-10**: algoritmo de detección de setups sobre las barras que ya trae `getStockBars`/`getCryptoBars`, usando el universo de `UniverseService.getCurrent()`.
- **TAI-11**: Spring AI para la explicación en lenguaje simple (reemplaza el `LlmClient` manual del spike).

## TAI-9: universo dinámico de activos

Endpoints:
```
GET  /universe            # universo actual (en memoria, calculado la última vez)
POST /universe/refresh    # fuerza un recálculo ahora (no hace falta esperar al cron)
```

Se recalcula automáticamente **una vez al día, a las 6 AM** (antes de la apertura del mercado de EE.UU.) — configurable con `UNIVERSE_REFRESH_CRON`. No hace falta más frecuencia: el ranking de mayores empresas por capitalización no cambia hora a hora.

**Fuentes de datos:**
- **Acciones (top 130 por default):** dos piezas combinadas — la lista de símbolos candidatos viene de los constituyentes del S&P 500 (CSV público en GitHub, sin API key), y la capitalización real de cada uno viene de [Finnhub](https://finnhub.io) (free tier: 60 requests/minuto, sin tarjeta). *(FMP se descartó como fuente: jubiló el endpoint de stock screener del plan gratuito — ahora requiere plan pago.)*
- **Cripto (top 30 por default):** con los propios datos de Alpaca — se listan los pares cripto que Alpaca soporta y se rankean por volumen de 24hs usando `getCryptoBars`. No hace falta ninguna fuente externa acá.

**⚠️ El recálculo de acciones tarda ~9-10 minutos.** Con ~500 símbolos del S&P 500 y el límite de 60 req/min de Finnhub, el código pacea las requests a propósito (una cada 1.1 segundos) para no pasarse del límite. Como se corre una vez al día a las 6 AM, esto no es un problema en producción — pero si lo probás manualmente con `POST /universe/refresh`, esperá varios minutos antes de que responda.

**Variables nuevas:**
```bash
export FINNHUB_API_KEY="tu_key_de_finnhub"
```
(Sacala gratis en [finnhub.io](https://finnhub.io/register), sin tarjeta — el dashboard te muestra la key apenas confirmás el mail.)

Opcionales (ya tienen default razonable):
```bash
export UNIVERSE_STOCK_TOP_N=130
export UNIVERSE_CRYPTO_TOP_N=30
```
