package com.tai.assistant.universe;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mantiene el universo de activos actual en memoria y lo recalcula automáticamente.
 *
 * Por qué una vez al día alcanza: la composición del top 100-150 por capitalización
 * de mercado no cambia hora a hora (una empresa no entra o sale del ranking de un día
 * para el otro salvo eventos excepcionales), y el top 30 cripto por volumen tampoco
 * varía tan drástico como para necesitar refrescarlo más seguido.
 *
 * Persistencia: MongoDB Atlas (colección "universe", un único documento con id="current").
 * El universo sobrevive a un reinicio del servidor — no hace falta esperar los ~9-10
 * minutos del refresh completo cada vez que se reinicia durante desarrollo. Se guarda
 * automáticamente después de cada refresh (manual o del cron diario) y se recarga
 * solo al arrancar la app.
 */
@Service
public class UniverseService {

    private final StockUniverseProvider stockProvider;
    private final CryptoUniverseProvider cryptoProvider;
    private final UniverseRepository repository;

    @Value("${tai.universe.stock-top-n:130}")
    private int stockTopN;

    @Value("${tai.universe.crypto-top-n:30}")
    private int cryptoTopN;

    private final AtomicReference<AssetUniverse> current = new AtomicReference<>(
            new AssetUniverse(List.of(), List.of(), null)
    );

    public UniverseService(StockUniverseProvider stockProvider, CryptoUniverseProvider cryptoProvider,
                            UniverseRepository repository) {
        this.stockProvider = stockProvider;
        this.cryptoProvider = cryptoProvider;
        this.repository = repository;
    }

    /** Al levantar la app, intenta cargar el último universo calculado desde MongoDB. */
    @PostConstruct
    public void loadFromDatabase() {
        try {
            repository.findById(UniverseDocument.CURRENT_ID).ifPresentOrElse(
                    doc -> {
                        current.set(doc.toDomain());
                        System.out.println("[UniverseService] Universo cargado desde MongoDB: "
                                + doc.stockSymbols().size() + " acciones + " + doc.cryptoSymbols().size()
                                + " cripto (calculado " + doc.calculatedAt() + ")");
                    },
                    () -> System.out.println("[UniverseService] No hay universo guardado todavía en MongoDB — arranca vacío.")
            );
        } catch (Exception e) {
            // Si Mongo no está disponible al arrancar (sin internet, URI mal configurada, etc.),
            // la app igual levanta — arranca con el universo vacío en memoria hasta el próximo refresh.
            System.err.println("[UniverseService] No se pudo conectar a MongoDB al arrancar: " + e.getMessage());
        }
    }

    public AssetUniverse getCurrent() {
        return current.get();
    }

    /** Recalcula el universo ahora mismo (se puede disparar manualmente vía POST /universe/refresh). */
    public AssetUniverse refresh() {
        List<String> stocks = stockProvider.isConfigured()
                ? stockProvider.fetchTopByMarketCap(stockTopN)
                : List.of();
        List<String> crypto = cryptoProvider != null
                ? cryptoProvider.fetchTopByVolume(cryptoTopN)
                : List.of();

        AssetUniverse universe = new AssetUniverse(stocks, crypto, Instant.now());
        current.set(universe);
        saveToDatabase(universe);
        return universe;
    }

    private void saveToDatabase(AssetUniverse universe) {
        try {
            repository.save(UniverseDocument.of(universe));
        } catch (Exception e) {
            // No persistir no debería tirar abajo un refresh que sí funcionó en memoria.
            System.err.println("[UniverseService] No se pudo guardar el universo en MongoDB: " + e.getMessage());
        }
    }

    /**
     * Recalcula automáticamente todos los días a las 6:00 AM (hora del servidor) —
     * antes de la apertura del mercado de EE.UU., para tener el universo fresco
     * cuando arranca el día de trading. Configurable con tai.universe.refresh-cron.
     */
    @Scheduled(cron = "${tai.universe.refresh-cron:0 0 6 * * *}")
    public void scheduledRefresh() {
        try {
            AssetUniverse u = refresh();
            System.out.println("[UniverseService] Universo recalculado: " + u.stockSymbols().size()
                    + " acciones + " + u.cryptoSymbols().size() + " cripto @ " + u.calculatedAt());
        } catch (Exception e) {
            System.err.println("[UniverseService] Error recalculando el universo: " + e.getMessage());
        }
    }
}
