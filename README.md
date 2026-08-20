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

**Se persiste en MongoDB Atlas** (colección `universe`, un único documento con `id="current"`). El universo sobrevive a un reinicio del servidor — no hace falta volver a esperar los ~9-10 min cada vez que parás y volvés a levantar la app durante desarrollo. Se guarda automáticamente después de cada refresh (manual o del cron diario) y se recarga solo al arrancar. Solo se recalcula desde cero cuando vos corrés `POST /universe/refresh` o cuando llegan las 6 AM.

**Variable nueva:**
```bash
export MONGODB_URI="mongodb+srv://TU_USUARIO:TU_PASSWORD@cluster0.eny5okp.mongodb.net/trading_assistant?appName=Cluster0"
```
(Sacá tu usuario/password de la base desde Atlas → Database Access. Notá el nombre de base `trading_assistant` agregado en el path — si no lo ponés, Mongo usa una base por default llamada `test`, mejor ser explícito.)

Si Mongo no está disponible al arrancar (sin internet, URI mal puesta), la app igual levanta — arranca con el universo vacío en memoria hasta el próximo refresh, no se cae.

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

## TAI-10: detección de setups (análisis técnico)

Endpoints:
```
GET  /setups        # últimos setups detectados (del último scan, en memoria)
POST /setups/scan   # fuerza un escaneo completo del universo ahora mismo
```

**Algoritmo (simple a propósito, para el MVP):** cruce de medias móviles — SMA(9) cruzando por encima de SMA(21) es la señal alcista. Cuando se detecta:
- **Entry price:** el último cierre.
- **Stop-loss:** 3% por debajo del entry.
- **Take-profit:** 6% por encima del entry (ratio riesgo/beneficio 1:2).
- **Nivel de riesgo:** BAJO/MEDIO/ALTO según la volatilidad reciente (desvío estándar de los últimos 10 cierres, como % del precio).

Es un heurístico simple, no un modelo sofisticado — si más adelante hace falta algo más elaborado, `TechnicalAnalysisService` es el único lugar que hay que tocar; el resto del pipeline no sabe ni le importa cómo se decide un setup.

**Frecuencia de escaneo:** cada 3 minutos por default (`DETECTION_SCAN_INTERVAL_MS`), respetando el límite de rate de Alpaca calculado en TAI-7. El primer escaneo espera 1 minuto después de levantar la app (`DETECTION_INITIAL_DELAY_MS`), para darle tiempo a que el universo tenga datos.

**Importante:** si el universo todavía está vacío (no corriste `POST /universe/refresh` ni pasaron las 6 AM del cron), el escaneo programado se salta ese ciclo sin romper nada — para probar esto de una, primero asegurate de tener el universo cargado (ver sección de TAI-9 arriba).

## TAI-11: explicación en lenguaje simple (Spring AI)

Cada setup detectado (TAI-10) se enriquece con una explicación en español generada por LLM, vía Spring AI apuntando a Groq — el mismo proveedor que se usó en el spike de latencia (TAI-5).

**Respuesta de `/setups` y `/setups/scan` ahora incluye:**
```json
{
  "setup": { "symbol": "NVDA", "entryPrice": 180.5, "stopLoss": 175.1, ... },
  "explanation": "NVIDIA muestra una señal alcista según el cruce de sus medias móviles..."
}
```

**Stack:** Spring Boot 3.5.16 + Spring AI 1.1.8 (GA estable) + JDK 21. *(Se evaluó Spring Boot 4.1 + Spring AI 2.0, pero Spring AI 2.0 todavía está en milestone, no en versión estable — se prefirió la combinación GA por sobre estar en la última versión.)*

**Variables:** reusa las mismas `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL` que ya tenías configuradas del spike — no hace falta agregar nada nuevo si ya las tenés en tu `.bashrc`.

**Manejo de errores:** si el LLM falla (rate limit, sin conexión, etc.), el setup detectado NO se pierde — queda con un mensaje de fallback en `explanation`, pero todos los datos técnicos (entry, stop-loss, take-profit) siguen disponibles igual.

## Qué sigue

- **TAI-12**: bot de Telegram para avisos inmediatos, usando `ExplainedSetup` como contenido del mensaje.
