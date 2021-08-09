/********* AnyPayCordova.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
@import AnyPay;
@import ExternalAccessory;


@interface AnyPayCordova : CDVPlugin {
  // Member variables go here.
}

//@property (nonatomic) AnyPay *anyPay;
@property (nonatomic, strong) AnyPayTerminal *terminal;
@property (nonatomic, strong) AnyPayEndpoint *endpoint;
@property (nonatomic, strong) AnyPayTransaction *refTransaction;
@property (nonatomic, strong) ANPCloudPOSTerminalMessage *message;
@property (nonatomic, strong) ANPCloudAPI *cloudAPI;
@property (nonatomic, copy) void (^onCardReaderConnected)(AnyPayCardReader *cardReader);
@property (nonatomic, copy) void (^onCardReaderDisconnected)(void);
@property (nonatomic, copy) void (^onCardReaderConnectionFailed)(ANPMeaningfulError *error);
@property (nonatomic, copy) void (^onCardReaderError)(ANPMeaningfulError *error);
@property (nonatomic, copy) void (^onTransactionMessageReceived)(ANPCloudPOSTerminalMessage *);
@property (nonatomic, copy) void (^onConfigUpdateMessageReceived)(ANPCloudPOSTerminalMessage *);
@property (nonatomic, copy) void (^onMessageReceived)(ANPCloudPOSTerminalMessage *);

- (void)initializeSDK:(CDVInvokedUrlCommand*)command;
- (void)initialize:(CDVInvokedUrlCommand*)command;
- (void)getSupportKey:(CDVInvokedUrlCommand*)command;
- (void)initializeTerminalWithEndpoint:(CDVInvokedUrlCommand*)command;
- (void)initializeTerminalFromCloud:(CDVInvokedUrlCommand*)command;
- (void)validateEndpointConfiguration:(CDVInvokedUrlCommand*)command;
- (void)setSessionToken:(CDVInvokedUrlCommand*)command;
- (void)authenticateTerminal:(CDVInvokedUrlCommand*)command;
- (void)applyLoggingConfiguration:(CDVInvokedUrlCommand*)command;
- (void)clearTerminalSavedState:(CDVInvokedUrlCommand*)command;
- (void)restoreTerminalState:(CDVInvokedUrlCommand*)command;
- (void)saveTerminalState:(CDVInvokedUrlCommand*)command;
- (void)startEMVSale:(CDVInvokedUrlCommand*)command;
- (void)startKeyedSale:(CDVInvokedUrlCommand*)command;
- (void)disconnect:(CDVInvokedUrlCommand*)command;
- (void)subscribeOnCardReaderConnected:(CDVInvokedUrlCommand*)command;
- (void)subscribeOnCardReaderDisconnected:(CDVInvokedUrlCommand*)command;
- (void)subscribeToCardReaderEvent:(CDVInvokedUrlCommand*)command;
- (void)subscribeOnCardReaderConnectFailed:(CDVInvokedUrlCommand*)command;
- (void)subscribeOnCardReaderError:(CDVInvokedUrlCommand*)command;
- (void)subscribeTerminalConnectionState:(CDVInvokedUrlCommand *)command;
- (void)unsubscribeTerminalConnectionState:(CDVInvokedUrlCommand *)command;

- (void)unsubscribeCardReaderCallbacks:(CDVInvokedUrlCommand*)command;

- (void)connectToBluetoothReader:(CDVInvokedUrlCommand*)command;
- (void)connectToBluetoothReaderWithID:command:(CDVInvokedUrlCommand *)command;
- (void)connectAudioReader:(CDVInvokedUrlCommand*)command;

//- (void)pollForMessage:(CDVInvokedUrlCommand *)command;
- (void)startReceivingTransactions:(CDVInvokedUrlCommand *)command;
- (void)stopReceivingTransactions:(CDVInvokedUrlCommand *)command;
- (void)subscribeToTerminalConnectionState:(CDVInvokedUrlCommand *)command;
- (void)unsubscribeToTerminalConnectionState:(CDVInvokedUrlCommand *)command;

- (void)acceptMessage:(ANPCloudPOSTerminalMessage *)message command:(CDVInvokedUrlCommand *)command;
- (void)rejectMessage:(ANPCloudPOSTerminalMessage *)message command:(CDVInvokedUrlCommand *)command;
- (void)finishedMessage:(ANPCloudPOSTerminalMessage *)message command:(CDVInvokedUrlCommand *)command;
- (void)acceptTransaction:(AnyPayTransaction *)transaction command:(CDVInvokedUrlCommand *)command;
- (void)updateTransaction:(AnyPayTransaction *)transaction command:(CDVInvokedUrlCommand *)command;
- (void)cancelTransaction:(CDVInvokedUrlCommand *)command;

@property (nonatomic) NSString *readerEventcallbackId;
@property (nonatomic) NSString *signatureRequiredCallbackId;
@property (nonatomic) NSString *terminalMessageCallbackId;
@property (nonatomic) NSString *terminalConnectionStateCallbackId;
@property (nonatomic) NSArray *availableBTReaderList;

@end

@implementation AnyPayCordova

// Deprecated Method
- (void)initializeSDK:(CDVInvokedUrlCommand*)command {
    self.terminal = [AnyPay initialise].terminal;
    self.endpoint = self.terminal.endpoint;

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE] callbackId:command.callbackId];
}

- (void)initialize:(CDVInvokedUrlCommand*)command {
    [AnyPay initialise];

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE] callbackId:command.callbackId];
}

- (void)initializeTerminal:(CDVInvokedUrlCommand*)command {
    self.terminal = [AnyPay initialise].terminal;
    self.endpoint = self.terminal.endpoint;

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self.terminal toDictionary]] callbackId:command.callbackId];
}

- (void)initializeTerminalWithEndpoint:(CDVInvokedUrlCommand*)command {
    __block CDVPluginResult* pluginResult = nil;
    NSDictionary *config = [command.arguments objectAtIndex:0];
    AnyPayEndpoint *endpoint = [self mappedEndpoint:config];
    self.terminal = [AnyPayTerminal initialize:endpoint];
    self.endpoint = self.terminal.endpoint;

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self.terminal toDictionary]] callbackId:command.callbackId];
}

- (void)initializeTerminalFromCloud:(CDVInvokedUrlCommand*)command {
    _cloudAPI = [ANPCloudAPI getInstance];

    __block CDVPluginResult* pluginResult = nil;
    NSString *activationCode = [command.arguments objectAtIndex:0];
    NSString *activationKey = [command.arguments objectAtIndex:1];

    self.terminal = [AnyPayTerminal initializeFromCloudWithActivationCode:activationCode activationKey:activationKey completionHandler:^(BOOL activated, ANPMeaningfulError * _Nullable error) {
        if (activated) {
            self.endpoint = self.terminal.endpoint;

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self.terminal toDictionary]];
        }
        else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error toDictionary]];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getSupportKey:(CDVInvokedUrlCommand *)command {
    NSString *passphrase = [command.arguments objectAtIndex:0];
    NSString *descriptor = [command.arguments objectAtIndex:1];

    __block CDVPluginResult* pluginResult = nil;

    if ([descriptor isKindOfClass:NSNull.class]) {
        descriptor = nil;
    }

    [AnyPay getSupportKey:passphrase descriptor:descriptor completionHandler:^(NSString *supportKey) {
        if (supportKey) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:supportKey];
        }
        else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"Unable to get Support Key"}];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)validateEndpointConfiguration:(CDVInvokedUrlCommand*)command {
    [self authenticateTerminal:command];
}

- (void)setSessionToken:(CDVInvokedUrlCommand*)command {
    __block CDVPluginResult* pluginResult = nil;
    NSString *token = [command.arguments objectAtIndex:0];

    if (self.endpoint && [self.endpoint isKindOfClass:ANPProPayEndpoint.class]) {
        [((ANPProPayEndpoint *)self.endpoint) setSessionToken:token];
    }
}

- (void)authenticateTerminal:(CDVInvokedUrlCommand*)command {
    __block CDVPluginResult* pluginResult = nil;
    NSDictionary *config = [command.arguments objectAtIndex:0];

    if ([self.endpoint isKindOfClass:ANPWorldnetEndpoint.class]) {
        ANPWorldnetEndpoint *endpoint = (ANPWorldnetEndpoint *)self.endpoint;

        if ([[config allKeys] containsObject:@"worldnetSecret"]) {
            endpoint.worldnetSecret = config[@"worldnetSecret"];
        }

        if ([[config allKeys] containsObject:@"worldnetTerminalID"]) {
            endpoint.worldnetTerminalID = config[@"worldnetTerminalID"];
        }

        if ([[config allKeys] containsObject:@"gatewayUrl"]) {
            endpoint.gatewayUrl = config[@"gatewayUrl"];
        }
    }
    else if ([self.endpoint isKindOfClass:ANPProPayEndpoint.class]) {
        ANPProPayEndpoint *endpoint = (ANPProPayEndpoint *)self.endpoint;

        if ([[config allKeys] containsObject:@"x509Cert"]) {
            endpoint.x509Certificate = config[@"x509Cert"];
        }

        if ([[config allKeys] containsObject:@"accountNum"]) {
            endpoint.accountNumber = config[@"accountNum"];
        }

        if ([[config allKeys] containsObject:@"jsonApiBaseUrl"]) {
            endpoint.gatewayUrl = config[@"jsonApiBaseUrl"];
        }

        if ([[config allKeys] containsObject:@"xmlApiBaseUrl"]) {
            endpoint.integrationServerURI = config[@"xmlApiBaseUrl"];
        }

        if ([[config allKeys] containsObject:@"certStr"]) {
            endpoint.certStr = config[@"certStr"];
        }

        if ([[config allKeys] containsObject:@"terminalId"]) {
            endpoint.terminalID = config[@"terminalId"];
        }
    }
    else if ([self.endpoint isKindOfClass:ANPPriorityPaymentsEndpoint.class]) {
        ANPPriorityPaymentsEndpoint *endpoint = (ANPPriorityPaymentsEndpoint *)self.endpoint;

        if ([[config allKeys] containsObject:@"username"]) {
            endpoint.consumerKey = config[@"username"];
        }

        if ([[config allKeys] containsObject:@"password"]) {
            endpoint.consumerSecret = config[@"password"];
        }

        if ([[config allKeys] containsObject:@"gatewayUrl"]) {
            endpoint.gatewayUrl = config[@"gatewayUrl"];
        }

        if ([[config allKeys] containsObject:@"consumerKey"]) {
            endpoint.consumerKey = config[@"consumerKey"];
        }

        if ([[config allKeys] containsObject:@"secret"]) {
            endpoint.consumerSecret = config[@"secret"];
        }

        if ([[config allKeys] containsObject:@"merchantId"]) {
            endpoint.merchantId = config[@"merchantId"];
        }
    }

    [self.endpoint authenticateTerminal:^(BOOL authenticated, ANPMeaningfulError * _Nullable error) {

        if (!error) {
            [self.terminal saveState];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:authenticated];
        }
        else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error toDictionary]];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)applyLoggingConfiguration:(CDVInvokedUrlCommand*)command {
    NSString *loggerType = [command.arguments objectAtIndex:0];
    NSDictionary *config = [command.arguments objectAtIndex:1];


    ANPLogger *logger1 = [ANPLogStream getLogger:loggerType];

    if (!logger1) {
        return;
    }

    ANPLogConfigurationProperties *config1 = logger1.configuration;//[ANPLogConfigurationProperties new];

//    if ([[config allKeys] containsObject:@"streamLogTo"]) {
//        config1.streamLogTo = config[@"streamLogTo"];
//    }

    if ([[config allKeys] containsObject:@"remoteLoggingEnabled"]) {
        config1.remoteLoggingEnabled = [config[@"remoteLoggingEnabled"] boolValue];
    }

    if ([[config allKeys] containsObject:@"realtimeLoggingEnabled"]) {
        config1.realtimeLoggingEnabled = [config[@"realtimeLoggingEnabled"] boolValue];
    }

    if ([[config allKeys] containsObject:@"logToFile"]) {
        config1.logToFile = [config[@"logToFile"] boolValue];
    }

    if ([[config allKeys] containsObject:@"logToConsole"]) {
        config1.logToConsole = [config[@"logToConsole"] boolValue];
    }

//    if ([[config allKeys] containsObject:@"batchingInterval"]) {
//        config1.logToConsole = [config[@"batchingInterval"] intValue];
//    }

    if ([[config allKeys] containsObject:@"logLevel"]) {
        config1.logLevel = config[@"logLevel"];
    }

    [logger1 applyConfiguration:config1];

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE] callbackId:command.callbackId];
}

- (void)saveTerminalState:(CDVInvokedUrlCommand*)command {
    [self.terminal saveState];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE] callbackId:command.callbackId];
}

- (void)restoreTerminalState:(CDVInvokedUrlCommand*)command {
    @try {
        self.terminal = [AnyPayTerminal restoreState];

        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self.terminal toDictionary]] callbackId:command.callbackId];
    } @catch (ANPTerminalNotActivatedException *exception) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"Terminal not initialized"}] callbackId:command.callbackId];
    }
}

- (void)clearTerminalSavedState:(CDVInvokedUrlCommand*)command {
    [AnyPayTerminal clearSavedState];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE] callbackId:command.callbackId];
}

- (void)startEMVSale:(CDVInvokedUrlCommand*)command {
    __block CDVPluginResult* pluginResult = nil;

    NSMutableDictionary *transactionDict = [command.arguments objectAtIndex:0];

    AnyPayTransaction *transaction = nil;

    @try {
        [transactionDict removeObjectForKey:@"cardReader"];
        transaction = [self createTransactionObject:transactionDict renameCustomProperty:YES];
    } @catch (NSException *exception) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"Invalid Transaction JSON sent"}] callbackId:command.callbackId];
    } @finally {

    }

    _refTransaction = transaction;


    if (_signatureRequiredCallbackId) {
        [transaction setOnSignatureRequired:^{
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:self.signatureRequiredCallbackId];
        }];
    }

    [transaction execute:^(ANPTransactionStatus status, ANPMeaningfulError * _Nullable error) {
        NSMutableDictionary *dict = ((NSDictionary *)[transaction performSelector:@selector(entireDictionary) withObject:nil]).mutableCopy;

        if ([[dict allKeys] containsObject:@"customProperties"]) {
            [dict setValue:dict[@"customProperties"] forKey:@"customFields"];
            [dict removeObjectForKey:@"customProperties"];
        }

        if ([[dict allKeys] containsObject:@"transactionTime"]) {
            [dict removeObjectForKey:@"transactionTime"];
        }

        if (!error || (transaction.gatewayResponse)) {

            if ([[dict allKeys] containsObject:@"gatewayResponse"]) {
                NSMutableDictionary *gResponseDict = ((NSDictionary *)dict[@"gatewayResponse"]).mutableCopy;

                if ([[gResponseDict allKeys] containsObject:@"transactionResponse"]) {
                    [gResponseDict removeObjectForKey:@"transactionResponse"];
                }

                if ([[gResponseDict allKeys] containsObject:@"transactionTime"]) {
                    [gResponseDict removeObjectForKey:@"transactionTime"];
                }

                dict[@"gatewayResponse"] = gResponseDict;
            }

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
        }
        else {
            [dict setValue:[error toDictionary] forKey:@"transactionError"];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:dict];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } cardReaderEvent:^(ANPMeaningfulMessage * _Nullable message) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message.message];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:self.readerEventcallbackId];
    }];
}

- (void)startKeyedTransaction:(CDVInvokedUrlCommand*)command {
    __block CDVPluginResult* pluginResult = nil;

    NSDictionary *transactionDict = [command.arguments objectAtIndex:0];

    AnyPayTransaction *transaction = nil;

    @try {
        transaction = [self createTransactionObject:transactionDict renameCustomProperty:YES];
    } @catch (NSException *exception) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"Invalid Transaction JSON sent"}] callbackId:command.callbackId];
    } @finally {

    }

    _refTransaction = transaction;

    if (((transaction.transactionType == ANPTransactionType_VOID) || (transaction.transactionType == ANPTransactionType_REFUND) || (transaction.transactionType == ANPTransactionType_REVERSEAUTH))) {

        if (transaction.externalID.length > 0) {
            transaction = [transaction createReversal];
        }
    }

    [transaction execute:^(ANPTransactionStatus status, ANPMeaningfulError * _Nullable error) {
        NSMutableDictionary *dict = ((NSDictionary *)[transaction performSelector:@selector(entireDictionary) withObject:nil]).mutableCopy;

        if ([[dict allKeys] containsObject:@"customProperties"]) {
            [dict setValue:dict[@"customProperties"] forKey:@"customFields"];
            [dict removeObjectForKey:@"customProperties"];
        }

        if ([[dict allKeys] containsObject:@"transactionTime"]) {
            [dict removeObjectForKey:@"transactionTime"];
        }

        if (!error || (transaction.gatewayResponse)) {

            if ([[dict allKeys] containsObject:@"gatewayResponse"]) {
                NSMutableDictionary *gResponseDict = ((NSDictionary *)dict[@"gatewayResponse"]).mutableCopy;

                if ([[gResponseDict allKeys] containsObject:@"transactionResponse"]) {
                    [gResponseDict removeObjectForKey:@"transactionResponse"];
                }

                if ([[gResponseDict allKeys] containsObject:@"transactionTime"]) {
                    [gResponseDict removeObjectForKey:@"transactionTime"];
                }

                dict[@"gatewayResponse"] = gResponseDict;
            }

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
        }
        else {
            [dict setValue:[error toDictionary] forKey:@"transactionError"];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:dict];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)disconnectReader:(CDVInvokedUrlCommand*)command {
    [[ANPCardReaderController sharedController] disconnectReader];
}

- (void)subscribeOnCardReaderConnected:(CDVInvokedUrlCommand*)command {

    self.availableBTReaderList = nil;

    _onCardReaderConnected = ^(AnyPayCardReader * _Nullable cardReader) {

        NSMutableDictionary *dict = [[cardReader valueForKey:@"deviceInfo"] mutableCopy];
        [dict setValue:[cardReader name] forKey:@"name"];

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    };

    [[ANPCardReaderController sharedController] subscribeOnCardReaderConnected:_onCardReaderConnected];
}

- (void)subscribeOnCardReaderDisconnected:(CDVInvokedUrlCommand*)command {

    self.availableBTReaderList = nil;

    _onCardReaderDisconnected = ^{

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Card Reader Disconnected"];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    };

    [[ANPCardReaderController sharedController] subscribeOnCardReaderDisConnected:_onCardReaderDisconnected];
}

- (void)subscribeToCardReaderEvent:(CDVInvokedUrlCommand*)command {
    _readerEventcallbackId = command.callbackId;
}

- (void)subscribeOnCardReaderConnectFailed:(CDVInvokedUrlCommand*)command {
    self.availableBTReaderList = nil;

    _onCardReaderConnectionFailed = ^(ANPMeaningfulError * _Nullable error) {

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:error.detail];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    };

    [[ANPCardReaderController sharedController] subscribeOnCardReaderConnectionFailed:_onCardReaderConnectionFailed];
}

- (void)subscribeOnCardReaderError:(CDVInvokedUrlCommand*)command {
    _onCardReaderError = ^(ANPMeaningfulError * _Nullable error) {

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:error.detail];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    };

    [[ANPCardReaderController sharedController] subscribeOnCardReaderError:_onCardReaderError];
}


- (void)unsubscribeCardReaderCallbacks:(CDVInvokedUrlCommand*)command {
    [[ANPCardReaderController sharedController] unsubscribeOnCardReaderError:_onCardReaderError];
    [[ANPCardReaderController sharedController] unsubscribeOnCardReaderConnected:_onCardReaderConnected];
    [[ANPCardReaderController sharedController] unsubscribeOnCardReaderDisConnected:_onCardReaderDisconnected];
    [[ANPCardReaderController sharedController] unsubscribeOnCardReaderConnectionFailed:_onCardReaderConnectionFailed];
}


- (void)connectToBluetoothReader:(CDVInvokedUrlCommand*)command {
    NSNumber *selectOnMultipleReadersAvailable = [command.arguments objectAtIndex:0];

    if ([selectOnMultipleReadersAvailable boolValue]) {
        [[ANPCardReaderController sharedController] connectBluetoothReader:^(NSArray<NSObject *> * _Nullable readers) {
            self.availableBTReaderList = readers;
            NSMutableArray *readerIDs = @[].mutableCopy;

            [readers enumerateObjectsUsingBlock:^(NSObject * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                if ([obj isKindOfClass:EAAccessory.class]) {
                    if ([obj valueForKey:@"serialNumber"]) {
                        [readerIDs addObject:[obj valueForKey:@"serialNumber"]];
                    }
                }
                else {
                    [readerIDs addObject:[obj valueForKey:@"name"]];
                }
            }];

            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:readerIDs];
            result.keepCallback = @1;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
    else {
        [[ANPCardReaderController sharedController] connectBluetoothReader:nil];
    }

}

- (void)connectToBluetoothReaderWithID:(CDVInvokedUrlCommand *)command {
    NSString *serial = [command.arguments objectAtIndex:0];

    if (serial && self.availableBTReaderList && self.availableBTReaderList.count > 0) {
        [self.availableBTReaderList enumerateObjectsUsingBlock:^(NSObject *  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            NSString *key = @"name";
            if ([obj isKindOfClass:EAAccessory.class]) {
                key = @"serialNumber";
            }

            if ([[obj valueForKey:key] isEqualToString:serial]) {
                [[ANPCardReaderController sharedController] connectToBluetoothReader:obj];

                *stop = TRUE;
                self.availableBTReaderList = nil;
            }
        }];
    }
}

- (void)connectAudioReader:(CDVInvokedUrlCommand*)command {
    [[ANPCardReaderController sharedController] connectAudioReader];
}

- (void)setOnSignatureRequired:(CDVInvokedUrlCommand *)command {
    _signatureRequiredCallbackId = command.callbackId;
}

- (void)proceed:(CDVInvokedUrlCommand *)command {
    NSArray *signaturePoints = [command.arguments objectAtIndex:0];
    ANPSignature *signature = [self pointsToSignatureArray:signaturePoints];

    ((ANPCardTransaction *)_refTransaction).signature = signature;
    [_refTransaction proceed];
}

- (void)updateSignature:(CDVInvokedUrlCommand *)command {
    NSArray *signaturePoints = [command.arguments objectAtIndex:0];
    ANPSignature *signature = [self pointsToSignatureArray:signaturePoints];

    if (_refTransaction && signature) {
        [_refTransaction updateWithSignature:signature resultHandler:^(BOOL sent, ANPMeaningfulError * _Nullable error) {
            CDVPluginResult *result = nil;
            if (sent) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
            }
            else {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsBool:FALSE];
            }

            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
    else {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsBool:FALSE];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }

}

- (void)fetchTransactions:(CDVInvokedUrlCommand *)command {
    if ([self.endpoint isKindOfClass:ANPProPayEndpoint.class] || [self.endpoint isKindOfClass:ANPPriorityPaymentsEndpoint.class]) {
        [((ANPProPayEndpoint *)self.endpoint) fetchTransactions:^(NSArray *transactions) {
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:transactions];
            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
    else if ([self.endpoint isKindOfClass:ANPWorldnetEndpoint.class]) {
        [((ANPWorldnetEndpoint *)self.endpoint) fetchTransactions:[command.arguments objectAtIndex:0] orderID:[command.arguments objectAtIndex:1] fromDate:nil toDate:nil responseHandler:^(NSArray<AnyPayTransaction *> * _Nullable transactions) {
            NSMutableArray<NSDictionary *> *ts = @[].mutableCopy;

            [transactions enumerateObjectsUsingBlock:^(AnyPayTransaction * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                [ts addObject:[obj toDictionary]];
            }];

            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:ts];
            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
}

- (void)adjustTip:(CDVInvokedUrlCommand *)command {
    if (_refTransaction && [self.endpoint isKindOfClass:ANPWorldnetEndpoint.class]) {
        [(ANPWorldnetEndpoint *)self.endpoint submitTipAdjustment:[[ANPTipAdjustmentLineItem alloc] initWithName:@"Tip" rate:[ANPAmount amountWithString:[command.arguments objectAtIndex:0]] surchargeCalculationMethod:ANPSurchargeCalculationMethod_FLAT_RATE] forTransaction:_refTransaction resultHandler:^(BOOL submitted, ANPMeaningfulError * _Nullable err) {

            CDVPluginResult *result = nil;
            if (submitted) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
            }
            else {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[err toDictionary]];
            }

            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
    else {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsBool:FALSE];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

- (void)sendReceipt:(CDVInvokedUrlCommand *)command {
    if (_refTransaction) {
        [_refTransaction sendReceiptToEmail:[command.arguments objectAtIndex:0] phone:nil resultHandler:^(BOOL sent, ANPMeaningfulError * _Nullable err) {
            CDVPluginResult *result = nil;
            if (sent) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
            }
            else {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[err toDictionary]];
            }

            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }
    else {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsBool:FALSE];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

- (ANPSignature *)pointsToSignatureArray:(NSArray *)signaturePoints {
    ANPSignature *signature = [ANPSignature new];
    signature.signaturePointsArray = @[].mutableCopy;

    [signaturePoints enumerateObjectsUsingBlock:^(NSArray *  _Nonnull sArray, NSUInteger idx, BOOL * _Nonnull stop) {
        [sArray enumerateObjectsUsingBlock:^(NSDictionary<NSString *, NSNumber *> * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            ANPDrawPath *drawPath = [ANPDrawPath new];

            if (idx == 0) {
                drawPath.start = [NSValue valueWithCGPoint:CGPointMake(obj[@"x"].floatValue, obj[@"y"].floatValue)];
            }
            else if (idx == (sArray.count - 1)) {
                drawPath.end = [NSValue valueWithCGPoint:CGPointMake(obj[@"x"].floatValue, obj[@"y"].floatValue)];
            }
            else {
                drawPath.move = [NSValue valueWithCGPoint:CGPointMake(obj[@"x"].floatValue, obj[@"y"].floatValue)];
            }

            [signature.signaturePointsArray addObject:drawPath];
        }];
    }];

    return signature;
}

- (ANPCardReaderInterface)getCardInterfaceFromArray:(NSArray *)interfaceArray {
    ANPCardReaderInterface cInterface = ANPCardReaderInterfaceSwipeOrInsert;
    NSString *cardInterfaces = [[interfaceArray valueForKey:@"description"] componentsJoinedByString:@" "];

    if (!cardInterfaces || (cardInterfaces.length > 0)) {
        if ([cardInterfaces containsString:@"TAP"] && [cardInterfaces containsString:@"SWIPE"] && [cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceSwipeOrInsertOrTap;
        }
        else if ([cardInterfaces containsString:@"TAP"] && [cardInterfaces containsString:@"SWIPE"] && ![cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceSwipeOrTap;
        }
        else if ([cardInterfaces containsString:@"TAP"] && [cardInterfaces containsString:@"INSERT"] && ![cardInterfaces containsString:@"SWIPE"]) {
            cInterface = ANPCardReaderInterfaceInsertOrTap;
        }
        else if ([cardInterfaces containsString:@"TAP"] && ![cardInterfaces containsString:@"SWIPE"] && ![cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceTap;
        }
        else if (![cardInterfaces containsString:@"TAP"] && [cardInterfaces containsString:@"SWIPE"] && [cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceSwipeOrInsert;
        }
        else if (![cardInterfaces containsString:@"TAP"] && ![cardInterfaces containsString:@"SWIPE"] && [cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceInsert;
        }
        else if (![cardInterfaces containsString:@"TAP"] && [cardInterfaces containsString:@"SWIPE"] && ![cardInterfaces containsString:@"INSERT"]) {
            cInterface = ANPCardReaderInterfaceSwipe;
        }
    }

    return cInterface;
}

- (NSArray *)readerSupportedCardInterfaces {
    return [[ANPCardReaderInterfaces valueOf:[ANPCardReaderController sharedController].connectedReader.defaultCardInterface] componentsSeparatedByString:@" "];
}

- (NSArray *)intersectedArray:(NSArray *)array {
    NSMutableSet *set1 = [NSMutableSet setWithArray:array];
    NSSet *set2 = [NSSet setWithArray:[self readerSupportedCardInterfaces]];

    [set1 intersectSet:set2];

    return [set1 allObjects];
}

- (AnyPayEndpoint *)mappedEndpoint:(NSDictionary *)config {
    if (![[config allKeys] containsObject:@"provider"]) {
        return nil;
    }

    AnyPayEndpoint *endpoint = nil;
    NSString *provider = [config[@"provider"] lowercaseString];

    if ([provider isEqualToString:@"worldnet"]) {
        endpoint = [ANPWorldnetEndpoint new];
        if ([[config allKeys] containsObject:@"worldnetSecret"]) {
            ((ANPWorldnetEndpoint *)endpoint).worldnetSecret = config[@"worldnetSecret"];
        }

        if ([[config allKeys] containsObject:@"worldnetTerminalID"]) {
            ((ANPWorldnetEndpoint *)endpoint).worldnetTerminalID = config[@"worldnetTerminalID"];
        }

        if ([[config allKeys] containsObject:@"gatewayUrl"]) {
            ((ANPWorldnetEndpoint *)endpoint).gatewayUrl = config[@"gatewayUrl"];
        }
    }
    else if ([provider isEqualToString:@"propay"]) {
        endpoint = [ANPProPayEndpoint new];

        if ([[config allKeys] containsObject:@"x509Cert"]) {
            ((ANPProPayEndpoint *)endpoint).x509Certificate = config[@"x509Cert"];
        }

        if ([[config allKeys] containsObject:@"accountNum"]) {
            ((ANPProPayEndpoint *)endpoint).accountNumber = config[@"accountNum"];
        }

        if ([[config allKeys] containsObject:@"jsonApiBaseUrl"]) {
            ((ANPProPayEndpoint *)endpoint).gatewayUrl = config[@"jsonApiBaseUrl"];
        }

        if ([[config allKeys] containsObject:@"xmlApiBaseUrl"]) {
            ((ANPProPayEndpoint *)endpoint).integrationServerURI = config[@"xmlApiBaseUrl"];
        }

        if ([[config allKeys] containsObject:@"certStr"]) {
            ((ANPProPayEndpoint *)endpoint).certStr = config[@"certStr"];
        }

        if ([[config allKeys] containsObject:@"terminalId"]) {
            ((ANPProPayEndpoint *)endpoint).terminalID = config[@"terminalId"];
        }
    }
    else if ([provider isEqualToString:@"pps"]) {
        endpoint = [ANPPriorityPaymentsEndpoint new];

        if ([[config allKeys] containsObject:@"username"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).consumerKey = config[@"username"];
        }

        if ([[config allKeys] containsObject:@"password"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).consumerSecret = config[@"password"];
        }

        if ([[config allKeys] containsObject:@"gatewayUrl"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).gatewayUrl = config[@"gatewayUrl"];
        }

        if ([[config allKeys] containsObject:@"consumerKey"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).consumerKey = config[@"consumerKey"];
        }

        if ([[config allKeys] containsObject:@"secret"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).consumerSecret = config[@"secret"];
        }

        if ([[config allKeys] containsObject:@"merchantId"]) {
            ((ANPPriorityPaymentsEndpoint *)endpoint).merchantId = config[@"merchantId"];
        }
    }

    return endpoint;
}

/*- (void)pollForMessage:(CDVInvokedUrlCommand *)command {
    [self.cloudAPI getMessages:self.terminal completionHandler:^(ANPCloudPOSTerminalMessage * _Nonnull message, ANPMeaningfulError * _Nullable error) {
        if (error) {
            NSMutableDictionary *errorMessage = [error toDictionary].mutableCopy;

            if ([[errorMessage allKeys] containsObject:@"message"] && [((NSString *)errorMessage[@"message"]) containsString:@"(404)"]) {
                errorMessage[@"errorCode"] = @"404";
                errorMessage[@"message"] = @"Not Found";
                errorMessage[@"detail"] = @"No Message available";
            }

            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorMessage];
            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
        }

        self.message = message;

        NSMutableDictionary *messageDict = [message toDictionary].mutableCopy;

        NSMutableDictionary *tdict = ((NSDictionary *)[message.transaction performSelector:@selector(entireDictionary) withObject:nil]).mutableCopy;
        if ([[tdict allKeys] containsObject:@"customProperties"]) {
            [tdict setValue:tdict[@"customProperties"] forKey:@"customFields"];
            [tdict removeObjectForKey:@"customProperties"];
        }

        [messageDict setValue:tdict forKey:@"transaction"];

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:messageDict];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}*/

- (void)invokeTerminalMessageCallback:(ANPCloudPOSTerminalMessage *)message {
    if (!message) {
        NSMutableDictionary *errorMessage = @{}.mutableCopy;

        errorMessage[@"errorCode"] = @"404";
        errorMessage[@"message"] = @"Not Found";
        errorMessage[@"detail"] = @"No Message available";

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorMessage];
        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:self.terminalMessageCallbackId];
        return;
    }


    NSMutableDictionary *messageDict = [message toDictionary].mutableCopy;

    NSMutableDictionary *tdict = ((NSDictionary *)[message.transaction performSelector:@selector(entireDictionary) withObject:nil]).mutableCopy;
    if ([[tdict allKeys] containsObject:@"customProperties"]) {
        [tdict setValue:tdict[@"customProperties"] forKey:@"customFields"];
        [tdict removeObjectForKey:@"customProperties"];
    }

    [messageDict setValue:tdict forKey:@"transaction"];

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:messageDict];
    result.keepCallback = @1;
    [self.commandDelegate sendPluginResult:result callbackId:self.terminalMessageCallbackId];
}

- (void)startReceivingTransactions:(CDVInvokedUrlCommand *)command {
    self.terminalMessageCallbackId = command.callbackId;
    [self addMessageSubscribers];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] start];
}

- (void)addMessageSubscribers {
    __weak AnyPayCordova *weakSelf = self;

    _onMessageReceived = ^(ANPCloudPOSTerminalMessage *message) {
        __strong AnyPayCordova *strongSelf = weakSelf;

        if (message) {
            [message accept];
            [strongSelf invokeTerminalMessageCallback:message];
        }
    };

    _onTransactionMessageReceived = ^(ANPCloudPOSTerminalMessage *message) {
        __strong AnyPayCordova *strongSelf = weakSelf;

        if (message) {
            [strongSelf invokeTerminalMessageCallback:message];
        }
    };

    _onConfigUpdateMessageReceived = ^(ANPCloudPOSTerminalMessage *message) {
        __strong AnyPayCordova *strongSelf = weakSelf;

        strongSelf.message = message;

        [[AnyPayTerminal getInstance] overwriteConfiguration:message.terminal];

        [message accept];
        [message finished];

        if (message) {
            [strongSelf invokeTerminalMessageCallback:message];
        }
    };

    [[ANPCloudPosTerminalMessageQueue sharedInstance] subscribeToMessagesOfType:@"MESSAGE" subscriber:_onMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] subscribeToMessagesOfType:@"NEW_TRANSACTION" subscriber:_onTransactionMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] subscribeToMessagesOfType:@"CANCEL_TRANSACTION" subscriber:_onTransactionMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] subscribeToMessagesOfType:@"CONFIG_CHANGED" subscriber:_onConfigUpdateMessageReceived];
}

- (void)removeMessageSubscribers {
    [[ANPCloudPosTerminalMessageQueue sharedInstance] unsubscribeToMessagesOfType:@"MESSAGE" subscriber:_onMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] unsubscribeToMessagesOfType:@"NEW_TRANSACTION" subscriber:_onTransactionMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] unsubscribeToMessagesOfType:@"CANCEL_TRANSACTION" subscriber:_onTransactionMessageReceived];
    [[ANPCloudPosTerminalMessageQueue sharedInstance] unsubscribeToMessagesOfType:@"CONFIG_CHANGED" subscriber:_onConfigUpdateMessageReceived];

    _onMessageReceived = nil;
    _onTransactionMessageReceived = nil;
    _onConfigUpdateMessageReceived = nil;
}

- (void)stopReceivingTransactions:(CDVInvokedUrlCommand *)command {
    [[ANPCloudPosTerminalMessageQueue sharedInstance] stop];
    [self removeMessageSubscribers];
    self.terminalMessageCallbackId = nil;
}

- (void)subscribeTerminalConnectionState:(CDVInvokedUrlCommand *)command {
    self.terminalConnectionStateCallbackId = command.callbackId;

    __weak AnyPayCordova *weakSelf = self;
    [ANPCloudPosTerminalMessageQueue sharedInstance].onConnectionStatusChanged = ^(ANPCloudPosTerminalConnectionStatus status) {

        NSString *stringStatus = nil;

        switch (status) {
            case CloudPosTerminalConnection_CONNECTED:
                stringStatus = @"CONNECTED";
                break;

            case CloudPosTerminalConnection_CONNECTING:
                stringStatus = @"CONNECTING";
                break;
            case CloudPosTerminalConnection_RECONNECTING:
                stringStatus = @"RECONNECTING";
                break;

            case CloudPosTerminalConnection_DISCONNECTED:
                stringStatus = @"DISCONNECTED";
                break;
            case CloudPosTerminalConnection_DISCONNECTING:
                stringStatus = @"DISCONNECTING";
                break;

            default:
                break;
        }

        __strong AnyPayCordova *strongSelf = weakSelf;

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:stringStatus];

        result.keepCallback = @1;
        [self.commandDelegate sendPluginResult:result callbackId:strongSelf.terminalConnectionStateCallbackId];
    };
}

- (void)unsubscribeTerminalConnectionState:(CDVInvokedUrlCommand *)command {
    self.terminalConnectionStateCallbackId = nil;
    [ANPCloudPosTerminalMessageQueue sharedInstance].onConnectionStatusChanged = nil;
}

- (void)updateTransaction:(CDVInvokedUrlCommand *)command {
    NSDictionary *config = [command.arguments objectAtIndex:0];
    AnyPayTransaction *transaction = [self createTransactionObject:config renameCustomProperty:NO];

    [self.cloudAPI updateTransaction:transaction terminal:self.terminal completionHandler:^(AnyPayTransaction *tr, ANPMeaningfulError *error) {
        CDVPluginResult *result;

        if (!error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[tr toDictionary]];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error toDictionary]];
        }

        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)acceptTransaction:(CDVInvokedUrlCommand *)command {
    NSDictionary *config = [command.arguments objectAtIndex:0];
    AnyPayTransaction *transaction = [self createTransactionObject:config renameCustomProperty:NO];

    if (transaction.status == ANPTransactionStatus_QUEUED) {
        transaction.status = ANPTransactionStatus_PROCESSING;
    }

    [self.cloudAPI acceptTransaction:transaction terminal:self.terminal completionHandler:^(BOOL success, ANPMeaningfulError *error) {
        CDVPluginResult *result;

        if (success) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:success];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error toDictionary]];
        }

        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)acceptMessage:(CDVInvokedUrlCommand *)command {
    NSDictionary *config = [command.arguments objectAtIndex:0];

    NSError *errs;
    ANPCloudPOSTerminalMessage *message = [[ANPCloudPOSTerminalMessage alloc] initWithDictionary:config error:&errs];
    [message accept];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:YES];
    result.keepCallback = @0;
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)rejectMessage:(CDVInvokedUrlCommand *)command {
    NSDictionary *config = [command.arguments objectAtIndex:0];

    NSError *errs;
    ANPCloudPOSTerminalMessage *message = [[ANPCloudPOSTerminalMessage alloc] initWithDictionary:config error:&errs];

    [message reject];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:YES];
    result.keepCallback = @0;
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)finishedMessage:(CDVInvokedUrlCommand *)command {
    NSDictionary *config = [command.arguments objectAtIndex:0];

    NSError *errs;
    ANPCloudPOSTerminalMessage *message = [[ANPCloudPOSTerminalMessage alloc] initWithDictionary:config error:&errs];

    [message finished];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:YES];
    result.keepCallback = @0;
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)submitDeviceInfo:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult* pluginResult = nil;
    NSDictionary *readerInfo = [command.arguments objectAtIndex:0];

    [self.cloudAPI submitDeviceInfo:readerInfo terminal:self.terminal completionHandler:^(BOOL success, ANPMeaningfulError *error) {
        CDVPluginResult *result;

        if (success) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:success];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error toDictionary]];
        }

        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (AnyPayTransaction *)createTransactionObject:(NSDictionary *)transactionDict renameCustomProperty:(BOOL)rename {
//    AnyPayTransaction *transaction = [[AnyPayTransaction alloc] initWithType:[ANPTransactionTypes enumValue:transactionDict[@"transactionType"]]];

    NSError *err = nil;
    AnyPayTransaction *transaction = [[AnyPayTransaction alloc] initWithDictionary:transactionDict error:&err];

//    [transaction setValuesForKeysWithDictionary:transactionDict];

//    if ([[transactionDict allKeys] containsObject:@"cardInterfaceModes"]) {
//        NSArray *cardInterfaceArr = [self intersectedArray:transactionDict[@"cardInterfaceModes"]];
//        transaction.cardReader.selectedCardInterface = [self getCardInterfaceFromArray:cardInterfaceArr];
//    }

//    if ([[transactionDict allKeys] containsObject:@"totalAmount"]) {
//        if (transactionDict[@"totalAmount"] != [NSNull null]) {
//            transaction.totalAmount = [ANPAmount amountWithString:[transactionDict[@"totalAmount"] stringValue]];
//        }
//    }
//
//    if ([[transactionDict allKeys] containsObject:@"subtotal"]) {
//        if (transactionDict[@"subtotal"] != [NSNull null]) {
//            transaction.subtotal = [ANPAmount amountWithString:[transactionDict[@"subtotal"] stringValue]];
//        }
//    }
//
//    if ([[transactionDict allKeys] containsObject:@"tax"]) {
//        if (transactionDict[@"tax"] != [NSNull null]) {
//            transaction.tax = [ANPAmount amountWithString:[transactionDict[@"tax"] stringValue]];
//        }
//
//    }
//
//    if ([[transactionDict allKeys] containsObject:@"tip"]) {
//        if (transactionDict[@"tip"] != [NSNull null]) {
//            transaction.tip = [ANPAmount amountWithString:[transactionDict[@"tip"] stringValue]];
//        }
//
//    }
//
//    if ([[transactionDict allKeys] containsObject:@"fee"]) {
//        if (transactionDict[@"fee"] != [NSNull null]) {
//            transaction.fee = [ANPAmount amountWithString:[transactionDict[@"fee"] stringValue]];
//        }
//
//    }

    if ([[transactionDict allKeys] containsObject:@"externalId"]) {
        if (transactionDict[@"externalId"] != [NSNull null]) {
            transaction.externalID = transactionDict[@"externalId"];
        }
    }

    if ([[transactionDict allKeys] containsObject:@"orderId"]) {
        if (transactionDict[@"orderId"] != [NSNull null]) {
            transaction.orderID = transactionDict[@"orderId"];
        }
    }

    if ([[transactionDict allKeys] containsObject:@"cardholderName"]) {
        if (transactionDict[@"cardholderName"] != [NSNull null]) {
            transaction.cardHolderName = transactionDict[@"cardholderName"];
        }
    }

    if (rename) {
        if ([[transactionDict allKeys] containsObject:@"customFields"]) {
            if (transactionDict[@"customFields"] != [NSNull null]) {
                [((NSDictionary *)transactionDict[@"customFields"]) enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
                    [transaction addCustomProperty:key value:obj];
                }];
            }
        }

        [transaction removeCustomProperty:@"customFields"];
    }

    //Due to issue with corepay currency calculation, AnyPay skips auto-set of currency
//    if ([[transactionDict allKeys] containsObject:@"currency"]) {
//        if (transactionDict[@"currency"] != [NSNull null]) {
//            transaction.currency = transactionDict[@"currency"];
//        }
//    }

    return transaction;
}

- (void)setCloudFlavor:(CDVInvokedUrlCommand *)command {
    NSString *flavor = [command.arguments objectAtIndex:0];
    [ANPCloudAPI setFlavor:flavor];
}

- (void)cancelTransaction:(CDVInvokedUrlCommand *)command {
    if (!self.refTransaction) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"No Transaction to cancel"}];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }

    if ([[self.refTransaction.customProperties allKeys] containsObject:@"ReaderProcessingStarted"]) {
        if (((NSNumber *)self.refTransaction.customProperties[@"ReaderProcessingStarted"]).boolValue == TRUE) {
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"message" : @"Transaction in non cancellable state", @"detail" : @"Transaction could not be cancelled"}];
            result.keepCallback = @0;
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }
    }
    else {
        [self.refTransaction cancel];
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Cancellation Started"];
        result.keepCallback = @0;
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

@end
