package ru.rbt.barsgl.ejb.repository.props;

/**
 * Created by Ivan Sevastyanov on 29.09.2016.
 */
public enum ConfigProperty {
    SyncPieceCount("sync.piece.count")
    , SyncPieceConcurrency("sync.piece.concurrency")
    , SyncBalturConcurrency("sync.baltur.concurrency")
    , SyncIcrementMaxGLPdCount("sync.incr.max.glpd.count")
    , TransitAccTypeDebit("client.transit.acctype.dt")
    , TransitAccTypeCredit("client.transit.acctype.ct")
    , PdConcurency("pd.cuncurency")
    , ClientTransitReprocess("client.transit.reprocess");

    private String value;

    ConfigProperty(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
