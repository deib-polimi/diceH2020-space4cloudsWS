package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.AbstractConnectionSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 11/02/16./**
 * Configuration class for SPN solver/server
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "QN")
final class QNSettings extends AbstractConnectionSettings {

    private Double significance = 0.01;
    private QueueingNetworkModel model = QueueingNetworkModel.SIMPLE;

    private QNSettings(QNSettings that) {
        super(that);
        significance = that.getSignificance();
        model = that.getModel();
    }

    // Used by Spring Boot
    @SuppressWarnings("unused")
    QNSettings() {
        super();
    }

    @Override
    public ConnectionSettings shallowCopy() {
        return new QNSettings(this);
    }
}
