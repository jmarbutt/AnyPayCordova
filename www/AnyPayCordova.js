var exec = require('cordova/exec');

var terminal = null;
var refTransaction = null;

exports.initialize = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'initializeSDK', []);
};

exports.initializeSDK = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'initializeSDK', []);
};

exports.getSupportKey = function (passphrase, descriptor, success, error) {
    exec(success, error, 'AnyPayCordova', 'getSupportKey', [passphrase, descriptor]);
};

exports.validateEndpointConfiguration = function (arg0, success, error) {
    exec(success, error, 'AnyPayCordova', 'validateEndpointConfiguration', [arg0]);
};

exports.authenticateTerminal = function (arg0, success, error) {
    exec(success, error, 'AnyPayCordova', 'authenticateTerminal', [arg0]);
};

exports.connectToBluetoothReader = function (selectOnMultipleReadersAvailable, success, error) {
    exec(success, error, 'AnyPayCordova', 'connectToBluetoothReader', [selectOnMultipleReadersAvailable]);
};

exports.connectToAvailableReaderWithID = function (readerID, success, error) {
    exec(success, error, 'AnyPayCordova', 'connectToBluetoothReaderWithID', [readerID]);
};

exports.connectAudioReader = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'connectAudioReader', []);
};

exports.subscribeOnCardReaderConnected = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'subscribeOnCardReaderConnected', []);
};

exports.subscribeOnCardReaderDisconnected = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'subscribeOnCardReaderDisconnected', []);
};

exports.subscribeOnCardReaderConnectFailed = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'subscribeOnCardReaderConnectFailed', []);
};

exports.subscribeOnCardReaderError = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'subscribeOnCardReaderError', []);
};

exports.unsubscribeCardReaderCallbacks = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'unsubscribeCardReaderCallbacks', []);
};

exports.disconnectReader = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'disconnectReader', []);
};


// ------------------------------------ //


exports.AnyPayTerminal = function() {
    this.uuid = null;
    this.id = null;
    this.parentID = null;
    this.portfolio = null;
    this.dateCreated = null;
    this.dateModified = null;
    this.configuration = {};
    this.activationCode = null;
    this.sessionKey = null;
    this.operator = null;
    this.sessionKeyExpiry = null;
    this.sessionKeySalt = null;
    this.secretKey = null;
    this.status = null;
    this.endpoint = null;
};

exports.AnyPayTerminal.initialize = function (successCallback, errorCallback) {

    responseCallback = function (response) {
        terminal = response;

        successCallback(response);
    };

    exec(responseCallback, errorCallback, 'AnyPayCordova', 'initializeTerminal', []);
};

exports.AnyPayTerminal.initialize = function (endpoint, successCallback, errorCallback) {
    responseCallback = function (response) {
        terminal = response;

        successCallback(response);
    };

    exec(responseCallback, errorCallback, 'AnyPayCordova', 'initializeTerminalWithEndpoint', [endpoint]);
};

exports.AnyPayTerminal.initializeFromCloud = function (activationCode, activationKey, success, error) {
    responseCallback = function (response) {
        terminal = response;

        success(response);
    };

    exec(responseCallback, error, 'AnyPayCordova', 'initializeTerminalFromCloud', [activationCode, activationKey]);
};

exports.AnyPayTerminal.restoreState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'restoreTerminalState', []);
};

exports.AnyPayTerminal.prototype.clearSavedState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'clearTerminalSavedState', []);
};

exports.AnyPayTerminal.prototype.saveState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'saveTerminalState', []);
};

exports.restoreTerminalState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'restoreTerminalState', []);
};

exports.clearTerminalSavedState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'clearTerminalSavedState', []);
};

exports.saveTerminalState = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'saveTerminalState', []);
};

// ------------------------------------ //

exports.WorldnetEndpoint = function() {
    this.worldnetTerminalID = null;
    this.worldnetSecret = null;
    this.provider = 'worldnet';
    this.gatewayUrl = null;
    this.enableJsonPassthru = null;
};

exports.ProPayEndpoint = function() {
    this.x509Cert = null;
    this.certStr = null;
    this.xmlApiBaseUrl = null;
    this.jsonApiBaseUrl = null;
    this.accountNum = null;
    this.terminalId = null;
    this.enableJsonPassthru = true;
    this.provider = 'propay';
    this.sessionToken = null;
};

exports.ProPayEndpoint.prototype.setSessionToken = function (arg0, success, error) {
    exec(success, error, 'AnyPayCordova', 'setSessionToken', [arg0]);
};

exports.ProPayEndpoint.prototype.getSessionToken = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'getSessionToken', []);
};

exports.PriorityPaymentsEndpoint = function() {
    this.username = null;
    this.password = null;
    this.consumerKey = null;
    this.secret = null;
    this.merchantId = null;
    this.pinCaptureCapability = null;
    this.deviceInputCapability = null;
    this.enableJsonPassthru = true;
    this.provider = 'pps';
    this.gatewayUrl = null
};

exports.worldnetConfiguration = function() {
    this.worldnetTerminalID = null;
    this.worldnetSecret = null;
    this.provider = 'worldnet';
    this.gatewayUrl = null;
    this.enableJsonPassthru = null;
};

exports.propayConfiguration = function() {
    this.x509Cert = null;
    this.certStr = null;
    this.xmlApiBaseUrl = null;
    this.jsonApiBaseUrl = null;
    this.accountNum = null;
    this.terminalId = null;
    this.enableJsonPassthru = true;
    this.provider = 'propay';
    this.sessionToken = null;
};

exports.priorityPaymentsConfiguration = function() {
    this.username = null;
    this.password = null;
    this.consumerKey = null;
    this.secret = null;
    this.merchantId = null;
    this.pinCaptureCapability = null;
    this.deviceInputCapability = null;
    this.enableJsonPassthru = true;
    this.provider = 'pps';
    this.gatewayUrl = null
};

exports.loggingConfiguration = function() {
    this.remoteLoggingEnabled = false;
    this.realtimeLoggingEnabled = false;
    this.logToFile = false;
    this.logToConsole = true;
    this.batchingInterval = 15;
    this.streamLogTo = null;
    this.logLevel = "ERROR";
};

exports.AnyPayTransaction = function() {
    this.type = null;
    this.totalAmount = null;
    this.subtotal = null;
    this.tax = null;
    this.currency = null;
    this.tip = null;
    this.fee = null;
    this.cardExpiryMonth = null;
    this.cardExpiryYear = null;
    this.maskedPAN = null;
    this.cardholderName = null;
    this.CVV2 = null;
    this.address = null;
    this.postalCode = null;
    this.cardNumber = null;
    this.cardInterfaceModes = ['SWIPE', 'TAP', 'INSERT', 'PINPAD'];
    this.approvedStatus = null;
    this.externalId = null;
    this.internalId = null;
    this.refTransactionID = null;
    this.signature = null;
    this.invoiceNumber = null;
    this.customerEmail = null;
    this.customerPhone = null;
    this.approvedAmount = null;
    this.amountAvailableToRefund = null;
    this.customFields = {};
    this.notes = null;
    this.responseText = null;
    this.approvalCode = null;
    this.status = 'UNKNOWN';
    this.terminalId = null;
    this.orderId = null;
};

// ------------------------------------ //

exports.AnyPayTransaction.prototype.execute = function (successCallback, errorCallback, readerEventCallback) {

    refTransaction = this;

    responseCallback = function (response) {
        if (response) {
            Object.assign(refTransaction, response);

            if (response.hasOwnProperty('internalId'))
                refTransaction.internalId = response.internalId;

            if (response.hasOwnProperty('externalId'))
                refTransaction.externalId = response.externalId;

            if (response.hasOwnProperty('internalID'))
                refTransaction.internalId = response.internalID;

            if (response.hasOwnProperty('externalID'))
                refTransaction.externalId = response.externalID;

            if (response.hasOwnProperty('trace'))
                delete response.trace;

            if (response.hasOwnProperty('endpoint'))
                delete response.endpoint;

            refTransaction.approvedStatus = response.approved;
            refTransaction.status = response.status;
        }


        successCallback(response);
    };

    rErrorCallback = function (response) {
        let error;

        if (response) {
            if (response.hasOwnProperty('transactionError')) {
                error = response.transactionError;
                delete response.transactionError;
            }

            Object.assign(refTransaction, response);
            refTransaction.approvedStatus = response.approved;
        }

        errorCallback(error);
    };

    if (readerEventCallback === undefined)
        exec(responseCallback, rErrorCallback, 'AnyPayCordova', 'startKeyedTransaction', [this]);
    else {
        exec(readerEventCallback, null, 'AnyPayCordova', 'subscribeToCardReaderEvent', []);
        exec(responseCallback, rErrorCallback, 'AnyPayCordova', 'startEMVSale', [this]);
    }
};

exports.AnyPayTransaction.prototype.setOnSignatureRequired = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'AnyPayCordova', 'setOnSignatureRequired', []);
};


exports.AnyPayTransaction.prototype.proceed = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'AnyPayCordova', 'proceed', [this.signature]);
};

exports.AnyPayTransaction.prototype.updateSignature = function (arg0, successCallback, errorCallback) {
    this.signature = arg0;
    exec(successCallback, errorCallback, 'AnyPayCordova', 'updateSignature', [this.signature]);

};

exports.AnyPayTransaction.prototype.adjustTip = function (arg0, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'AnyPayCordova', 'adjustTip', [arg0]);
};

exports.AnyPayTransaction.prototype.sendReceipt = function (arg0, arg1, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'AnyPayCordova', 'sendReceipt', [arg0, arg1]);
};

exports.AnyPayTransaction.prototype.addCustomField = function (key, value) {
    this.customFields[key] = value;
};

exports.AnyPayTransaction.prototype.cancel = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'AnyPayCordova', 'cancelTransaction', []);
};

exports.AnyPayTransaction.prototype.removeCustomField = function (key) {
    return delete this.customFields[key];
};


// ------------------------------------ //

exports.CloudPosTerminalMessage = function () {
    this.type = null;
    this.satatus = null;
    this.message = null;
    this.reason = null;
    this.title = null;
    this.transaction = new exports.AnyPayTransaction();
    this.selfDestruct = false;
    this.terminalUUID = null;
    this.transactionUUID = null;
    this.force = false;
};

exports.CloudPosTerminalMessage.prototype.accept = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'acceptMessage', [this]);
};

exports.CloudPosTerminalMessage.prototype.reject = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'rejectMessage', [this]);
};

exports.CloudPosTerminalMessage.prototype.finished = function (success, error) {
    exec(success, error, 'AnyPayCordova', 'finishedMessage', [this]);
};

exports.CloudAPI = function () {

};

exports.CloudAPI.acceptTransaction = function (transaction, success, error) {
    transaction.status = 'PROCESSING';
    exec(success, error, 'AnyPayCordova', 'acceptTransaction', [transaction]);
};

exports.CloudAPI.updateTransaction = function (transaction, success, error) {
    exec(success, error, 'AnyPayCordova', 'updateTransaction', [transaction]);
};


// ------------------------------------ //

exports.CloudPosTerminalMessageQueue = function () {
    this.listeners = {};
};

exports.CloudPosTerminalMessageQueue.prototype.subscribeForMessageOfType = function(type, listener) {
    if (this.listeners.hasOwnProperty(type)) {
        this.listeners[type].add(listener);
    } else {
        this.listeners[type] = new Set([listener]);
    }
};

exports.CloudPosTerminalMessageQueue.prototype.unSubscribeForMessageOfType = function(type, listener) {
    this.listeners[type].delete(listener);
};

exports.CloudPosTerminalMessageQueue.prototype.subscribeForTerminalConnectionState = function (listener) {
    exec(listener, null, 'AnyPayCordova', 'subscribeTerminalConnectionState', []);
};

exports.CloudPosTerminalMessageQueue.prototype.unsubscribeForTerminalConnectionState = function () {
    exec(null, null, 'AnyPayCordova', 'unsubscribeTerminalConnectionState', []);
};

exports.CloudPosTerminalMessageQueue.prototype.start = function () {

    const _this = this;
    responseCallback = function (response) {
        message = Object.assign(new cordova.plugins.AnyPayCordova.CloudPosTerminalMessage(), response);

        if (message.hasOwnProperty("transaction")) {
            let transaction = Object.assign(new cordova.plugins.AnyPayCordova.AnyPayTransaction(), message.transaction);
            message.transaction = transaction;
        }

        _this.listeners[message.type].values().next().value(message);
    };

    errCallback = function (err) {

    };

    exec(responseCallback, errCallback, 'AnyPayCordova', 'startReceivingTransactions', []);
};

exports.CloudPosTerminalMessageQueue.prototype.stop = function () {

    responseCallback = function (response) {
    };

    errCallback = function (err) {
    };

    exec(responseCallback, errCallback, 'AnyPayCordova', 'stopReceivingTransactions', []);
};

// ------------------------------------ //

exports.fetchTransactions = function (arg0, arg1, successCallback, errorCallback) {
    if ((arg1 === null) || (arg1 === undefined))
        arg1 = '';

    exec(successCallback, errorCallback, 'AnyPayCordova', 'fetchTransactions', [arg0, arg1]);
};

exports.applyLoggerConfiguration = function (arg0, arg1, success, error) {
    exec(success, error, 'AnyPayCordova', 'applyLoggingConfiguration', [arg0, arg1]);
};

exports.propayConfiguration.prototype.setSessionToken = function (arg0, success, error) {
    exec(success, error, 'AnyPayCordova', 'setSessionToken', [arg0]);
};
