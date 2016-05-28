package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
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
public class QNSettings extends AbstractConnectionSettings {

    private Double significance = 0.01;
    private QueueingNetworkModel model = QueueingNetworkModel.SIMPLE;

    public QNSettings(QNSettings that) {
        super(that);
        significance = that.getSignificance();
        model = that.getModel();
    }

    QNSettings() {
        super();
    }
}
