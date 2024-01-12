package com.coastal24.connectorservices.SavingApplicationinfo.controller;

import java.util.List;
import java.util.function.Function;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.getProductDetailsDecisionXAHandler;

import com.coastal24.connectorservices.SavingApplicationinfo.handler.bookSavingsAccountHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.GetLoanStatusHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.GetUserStatusHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.generatePDFHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.getDepositRateChangesHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.getLoansHandler;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.ValidateJointApplicantAddressDetailsHandler;
import com.xtensifi.connectorservices.common.logging.ConnectorLogging;
import com.xtensifi.connectorservices.common.workflow.ConnectorConfig;
import com.xtensifi.connectorservices.common.workflow.ConnectorHubService;
import com.xtensifi.connectorservices.common.workflow.ConnectorHubServiceImpl;
import com.xtensifi.connectorservices.common.workflow.ConnectorRequestData;
import com.xtensifi.connectorservices.common.workflow.ConnectorResponse;
import com.xtensifi.connectorservices.common.workflow.ConnectorState;
import com.xtensifi.dspco.ConnectorMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin
@Controller
@RequestMapping("/externalConnector/savingsApplicationapp/2.8")
public class savingsApplicationController extends ControllerBase {

    // ConnectorHubService is required for workflow methods
    private ConnectorHubService connectorHubService;

    private ConnectorLogging clog = new ConnectorLogging();

    @Bean
    ConnectorHubService connectorHubService() {
        return new ConnectorHubServiceImpl();
    }

    @Bean
    ConnectorConfig connectorConfig() {
        return new ConnectorConfig();
    }

    private Function<ConnectorState, ConnectorState> processRetrieveAccountList() {
        return connectorState -> {
            clog.info(connectorState.getConnectorMessage(), "processRetrieveAccountList");

            // Gather the list of responses, when only making 1 kiva call there should only
            // be one response
            List<ConnectorResponse> connectorResponseList = connectorState.getConnectorResponseList().getResponses();

            // This is how you capture the response
            String resp = "{\"response\": 1}";
            for (ConnectorResponse connectorResponse : connectorResponseList) {

                // This is how you retrieve the name of the connector
                String name = connectorResponse.getConnectorRequestData().getConnectorName();
                clog.info(connectorState.getConnectorMessage(), name);

                // This is how you capture the response
                String data = connectorResponse.getResponse();

                // Parse the response how ever you see fit
                resp = "{\"response\": " + data + "}";
                clog.info(connectorState.getConnectorMessage(), "Responce Data" + resp);
            }

            // This is required, and is how you set the response for a workflow method
            connectorState.setResponse(resp);

            return connectorState;
        };
    }

    RestTemplate restTemplate;

    /**
     * This method is required in order for your controller to pass health checks.
     * If the server cannot call awsping and get the expected response yur app will
     * not be active.
     *
     * @return the required ping-pong string
     */

    @CrossOrigin
    @GetMapping("/awsping")
    public String getAWSPing() {
        return "{ping: 'pong'}";
    }

    @CrossOrigin
    @PostMapping("/getProductDetailsDecisionXA")
    public ConnectorMessage getProductDetailsDecisionXA(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:getProductDetailsDecisionXA: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new getProductDetailsDecisionXAHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping("/getLoanStatus")
    public ConnectorMessage getStatusFutures(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:getLoanStatus: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new GetLoanStatusHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping("/bookSavingsAccount")
    public ConnectorMessage bookSavingsAccount(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:bookSavingsAccount: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new bookSavingsAccountHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping("/getLoans")
    public ConnectorMessage getLoans(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:getLoans: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new getLoansHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping("/generatePDF")
    public ConnectorMessage generatePDF(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:generatePDF: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new generatePDFHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping("/getDepositRateChanges")
    public ConnectorMessage getDepositRateChanges(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:getDepositRateChanges: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new getDepositRateChangesHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    // method to get userStatus from db and fetch data previously stored if exist.
    @CrossOrigin
    @PostMapping("/getUserStatus")
    public ConnectorMessage getUserStatus(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:getUserStatus: ";
        log.info(logPrefix + "BEGIN...");

        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix,
                connector,
                new GetUserStatusHandler());
        log.info(logPrefix + "END...");
        return connectorMessage;
    }

    @PostMapping("/validateJointApplicantAddressDetails")
    public ConnectorMessage validateJointApplicantAddressDetails(@RequestBody ConnectorMessage connector) {
        final String logPrefix = "DecisionXAController:validateJointApplicantAddressDetails: ";
        clog.info(connector, logPrefix + "BEGIN...");
        final ConnectorMessage connectorMessage = handleConnectorMessage(logPrefix, connector,
                new ValidateJointApplicantAddressDetailsHandler());
        clog.info(connector, logPrefix + "END...");
        return connectorMessage;
    }

    @CrossOrigin
    @PostMapping(path = "/retrieveAccountListRefresh", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> retrieveAccountListRefresh(@RequestBody final ConnectorMessage connectorMessage) {
        clog.info(connectorMessage, "Connector Message:-" + connectorMessage.toString());

        ResponseEntity.BodyBuilder responseEntity = ResponseEntity.status(HttpStatus.OK);
        try {
            connectorHubService
                    .executeConnector(connectorMessage,
                            new ConnectorRequestData("kivapublic", "1.0", "getAccountsRefresh"))
                    .thenApply(this.processRetrieveAccountList()).thenApplyAsync(connectorHubService.completeAsync())
                    .exceptionally(exception -> connectorHubService.handleAsyncFlowError(exception, connectorMessage,
                            "Error running TileSummary: " + exception.getMessage()));
        } catch (Exception ex) {
            clog.info(connectorMessage, "Error in retrieveAccountListRefresh :-" + ex.getMessage());
        }
        return responseEntity.build();
    }
}