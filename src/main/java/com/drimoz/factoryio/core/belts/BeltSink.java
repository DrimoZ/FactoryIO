package com.drimoz.factoryio.core.belts;

/**
 * Ce qui reçoit les items sortant d'un convoyeur.
 *
 * <p>Volontairement réduit à une question : « prends-tu cet item, sur cette voie ? ». C'est le
 * seul verbe dont le transport ait besoin, et c'est ce qui permet à
 * {@link BeltTransport} d'ignorer si l'aval est un autre convoyeur, un inventaire, ou un mur.
 *
 * <p>Un aval qui refuse toujours <b>est</b> un mur : il n'y a pas de cas « bloqué » à traiter
 * à part, la compression en découle.
 */
@FunctionalInterface
public interface BeltSink<T> {

    /**
     * @param lane voie d'origine ; l'aval doit la respecter, un item ne change pas de côté en
     *             franchissant une frontière de bloc
     * @return {@code true} si l'item a été pris — l'amont s'en dessaisit alors définitivement
     */
    boolean accept(int lane, T item);

    /** Un aval qui ne prend rien. */
    static <T> BeltSink<T> blocked() {
        return (lane, item) -> false;
    }
}
