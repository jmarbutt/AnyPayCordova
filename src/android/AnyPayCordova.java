package com.anywherecommerce.anypaycordova;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.AuthenticationListener;
import com.anywherecommerce.android.sdk.CloudPosTerminal;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessage;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessageQueue;
import com.anywherecommerce.android.sdk.Endpoint;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.Logger;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.TaskListener;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.TerminalNotInitializedException;
import com.anywherecommerce.android.sdk.Users;
import com.anywherecommerce.android.sdk.devices.CardInterface;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDeviceCardReaderController;
import com.anywherecommerce.android.sdk.endpoints.AnyPayReferenceTransaction;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.prioritypayments.PriorityPaymentsEndpoint;
import com.anywherecommerce.android.sdk.endpoints.propay.PropayEndpoint;
import com.anywherecommerce.android.sdk.endpoints.worldnet.WorldnetEndpoint;
import com.anywherecommerce.android.sdk.models.CloudPosTerminalConnectionStatus;
import com.anywherecommerce.android.sdk.models.CustomerDetails;
import com.anywherecommerce.android.sdk.models.DrawPoint;
import com.anywherecommerce.android.sdk.models.Signature;
import com.anywherecommerce.android.sdk.models.TaxLineItem;
import com.anywherecommerce.android.sdk.models.TipLineItem;
import com.anywherecommerce.android.sdk.models.TransactionStatus;
import com.anywherecommerce.android.sdk.models.TransactionType;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;
import com.anywherecommerce.android.sdk.util.Amount;
import com.anywherecommerce.android.sdk.logging.LogConfigurationProperties;
import com.anywherecommerce.android.sdk.LogStream;
import com.anywherecommerce.android.sdk.util.PostProcessingTypeAdapterFactory;
import com.bbpos.DecryptedData;
import com.bbpos.EmvSwipeDecrypt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.DialogInterface;
import android.os.Handler;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import android.bluetooth.BluetoothDevice;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.anywherecommerce.android.sdk.endpoints.anywherecommerce.CloudAPI;

/**
 * This class echoes a string called from JavaScript.
 */
public class AnyPayCordova extends CordovaPlugin {

    private CardReaderController cardReaderController;
    private CallbackContext readerConnectedCallbackContext;
    private CallbackContext readerDisconnectedCallbackContext;
    private CallbackContext readerConnectionFailedCallbackContext;
    private CallbackContext readerConnectionErrorCallbackContext;
    private CallbackContext transactionCallbackContext;
    private CallbackContext cardReaderEventCallbackContext;
    private CallbackContext signatureRequiredCallbackContext;
    private CallbackContext terminalMessageCallbackContext;
    private CallbackContext terminalConnectionStateCallbackContext;
    private GenericEventListenerWithParam<CardReader> cardReaderConnectedListener;
    private GenericEventListener cardReaderDisconnectedListener;
    private MeaningfulErrorListener cardReaderErrorListener;
    private MeaningfulErrorListener cardReaderConnectionFailureListener;
    GenericEventListenerWithParam<CloudPosTerminalConnectionStatus> OnTerminalConnectionStatusChanged;

    private AnyPayTransaction refTransaction;
    private Endpoint endpoint;
    private List<BluetoothDevice> availableReaders;

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("authenticateTerminal")) {
//             String message = args.getString(0);
            JSONObject configJSON = args.getJSONObject(0);
            this.authenticateTerminal(configJSON, callbackContext);
            return true;
        }
        else if (action.equals("validateEndpointConfiguration")) {
            //             String message = args.getString(0);
            JSONObject configJSON = args.getJSONObject(0);
            this.authenticateTerminal(configJSON, callbackContext);
            return true;
        }
        else if (action.equals("initialize")) {
            this.initialize(callbackContext);
            return true;
        }
        else if (action.equals("initializeSDK")) {
            this.initializeSDK(callbackContext);
            return true;
        }
        else if (action.equals("getSupportKey")) {
            String passphrase = args.getString(0);
            String descriptor = args.getString(1);

            this.getSupportKey(passphrase, descriptor, callbackContext);
            return true;
        }
        else if (action.equals("initializeTerminal")) {
            this.initializeTerminal(callbackContext);
            return true;
        }
        else if (action.equals("initializeTerminalWithEndpoint")) {
            JSONObject configJSON = args.getJSONObject(0);

            this.initializeTerminalWithEndpoint(configJSON, callbackContext);
            return true;
        }
        else if (action.equals("initializeTerminalFromCloud")) {
            String activationCode = args.getString(0);
            String activationKey = args.getString(1);

            this.initializeTerminalFromCloud(activationCode, activationKey, callbackContext);
            return true;
        }
        else if (action.equals("saveTerminalState")) {
            Terminal.getInstance().saveState();
            callbackContext.success();
            return true;
        }
        else if (action.equals("setSessionToken")) {
            if ((endpoint != null) && (endpoint instanceof PropayEndpoint)) {
                String token = args.getString(0);
                ((PropayEndpoint)endpoint).setSessionToken(token);
            }

            return true;
        }
        else if (action.equals("restoreTerminalState")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        Terminal.restoreState();
                        endpoint = Terminal.getInstance().getEndpoint();
                        JSONObject terminalObject = new JSONObject(Terminal.getInstance().serialize());
                        PluginResult result = new PluginResult(PluginResult.Status.OK, terminalObject);
                        result.setKeepCallback(false);
                        callbackContext.sendPluginResult(result);
                    }
                    catch (Exception e) {
                        callbackContext.error("Terminal not initialized");
                    }
                }
            });

            return true;
        }
        else if (action.equals("clearTerminalSavedState")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Terminal.getInstance().clearSavedState();
                    callbackContext.success();
                }
            });

            return true;
        }
        else if (action.equals("applyLoggingConfiguration")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    JSONObject configJSON = null;
                    try {
                        String type = args.getString(0);
                        configJSON = args.getJSONObject(1);
                        AnyPayCordova.this.applyLoggingConfiguration(type, configJSON, callbackContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return true;
        }
        else if (action.equals("connectToBluetoothReader")) {

            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        boolean val = args.getBoolean(0);
                        AnyPayCordova.this.connectToBluetoothReader(val, callbackContext);
                    }
                    catch (JSONException e) {

                    }
                }
            });

            return true;
        }
        else if (action.equals("connectToBluetoothReaderWithID")) {

            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        String id = args.getString(0);
                        AnyPayCordova.this.connectToBluetoothReaderWithID(id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return true;
        }
        else if (action.equals("connectAudioReader")) {

            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.connectAudioReader();
                }
            });

            return true;
        }
        else if (action.equals("startEMVSale")) {
            transactionCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        JSONObject transactionJSON = args.getJSONObject(0);

                        AnyPayCordova.this.startEMVSale(createTransactionObject(transactionJSON), callbackContext);
                    }
                    catch (Exception e) {

                    }
                }
            });

            return true;
        }
        else if (action.equals("startKeyedTransaction")) {
            transactionCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        JSONObject transactionJSON = args.getJSONObject(0);
                        AnyPayCordova.this.startKeyedTransaction(createTransactionObject(transactionJSON), callbackContext);
                    }
                    catch (Exception e) {
                        Logger.logException(e);
                    }
                }
            });

            return true;
        }
        else if (action.equals("subscribeOnCardReaderConnected")) {
            readerConnectedCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.subscribeOnCardReaderConnected(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("subscribeOnCardReaderDisconnected")) {
            readerDisconnectedCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.subscribeOnCardReaderDisconnected(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("subscribeOnCardReaderConnectFailed")) {
            readerConnectionFailedCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.subscribeOnCardReaderConnectFailed(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("subscribeOnCardReaderError")) {
            readerConnectionErrorCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.subscribeOnCardReaderError(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("subscribeToCardReaderEvent")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.subscribeToCardReaderEvent(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("unsubscribeCardReaderCallbacks")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.unsubscribeCardReaderCallbacks(callbackContext);
                }
            });

            return true;
        }
        else if (action.equals("disconnectReader")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.disconnectReader();
                }
            });

            return true;
        }
        else if (action.equals("proceed")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.proceed(jsonArraytoArrayList(args.getJSONArray(0)), callbackContext);
                    }
                    catch (Exception e) {
                        Logger.logException(e);
                    }
                }
            });

            return true;
        }
        else if (action.equals("setOnSignatureRequired")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.setOnSignatureRequired(callbackContext);
                    }
                    catch (Exception e) {

                    }
                }
            });

            return true;
        }
        else if (action.equals("disconnectReader")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    AnyPayCordova.this.disconnectReader();
                }
            });

            return true;
        }
        else if (action.equals("fetchTransactions")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.fetchTransactions(args.getInt(0), args.getString(1), callbackContext);
                    }
                    catch (Exception e){}
                }
            });

            return true;
        }
        else if (action.equals("adjustTip")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.adjustTip(args.getString(0), callbackContext);
                    }catch (Exception e){}
                }
            });

            return true;
        }
        else if (action.equals("updateSignature")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.updateSignature(jsonArraytoArrayList(args.getJSONArray(0)), callbackContext);
                    }catch (Exception e){}
                }
            });

            return true;
        }
        else if (action.equals("sendReceipt")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.sendReceipt(args.getString(0), args.getString(1), callbackContext);
                    }catch (Exception e){}
                }
            });

            return true;
        } else if (action.equals("setCloudFlavor")) {
            String flavor = args.getString(0);
            this.setCloudFlavor(flavor, callbackContext);
            return true;
        }
        else if (action.equals("acceptMessage")) {
            JSONObject jsonObject = args.getJSONObject(0);
            this.acceptMessage(jsonObject, callbackContext);
            return true;
        }
        else if (action.equals("finishedMessage")) {
            JSONObject jsonObject = args.getJSONObject(0);
            this.finishedMessage(jsonObject, callbackContext);
            return true;
        }
        else if (action.equals("rejectMessage")) {
            JSONObject jsonObject = args.getJSONObject(0);
            this.rejectMessage(jsonObject, callbackContext);
            return true;
        }
//        else if (action.equals("pollForMessage")) {
//            this.pollForMessage(callbackContext);
//            return true;
//        }
        else if (action.equals("startReceivingTransactions")) {
            this.startReceivingTransactions(callbackContext);
            return true;
        }
        else if (action.equals("stopReceivingTransactions")) {
            this.stopReceivingTransactions();
            return true;
        }
        else if (action.equals("subscribeTerminalConnectionState")) {
            this.subscribeToConnectionState(callbackContext);
            return true;
        }
        else if (action.equals("unsubscribeTerminalConnectionState")) {
            this.unsubscribeToConnectionState();
            return true;
        }
        else if (action.equals("acceptTransaction")) {
            JSONObject jsonObject = args.getJSONObject(0);
            this.acceptTransaction(jsonObject, callbackContext);
            return true;
        }
        else if (action.equals("updateTransaction")) {
            JSONObject jsonObject = args.getJSONObject(0);
            this.updateTransaction(jsonObject, callbackContext);
            return true;
        }
        else if (action.equals("cancelTransaction")) {
            this.cancelTransaction(callbackContext);
            return true;
        }

        return false;
    }

    private void initialize(CallbackContext callbackContext) {
        AnyPay.initialize(this.cordova.getActivity().getApplication());

        cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);

        callbackContext.success();
    }

    private void initializeSDK(CallbackContext callbackContext) {
        AnyPay.initialize(this.cordova.getActivity().getApplication());

        cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);

        Terminal.initialize();
        endpoint = Terminal.getInstance().getEndpoint();

        callbackContext.success();
    }

    private void getSupportKey(String passphrase, String descriptor, CallbackContext callbackContext) {
        AnyPay.getSupportKey(passphrase, descriptor, new RequestListener<String>() {
            @Override
            public void onRequestComplete(String s) {
                callbackContext.success(s);
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private void initializeTerminal(CallbackContext callbackContext) {
        Terminal.initialize();
        endpoint = Terminal.getInstance().getEndpoint();

        callbackContext.success();
    }

    private void initializeTerminalWithEndpoint(JSONObject configJSON, CallbackContext callbackContext) throws JSONException {

        Terminal.initialize(this.mappedEndpoint(configJSON));
        this.endpoint = Terminal.getInstance().getEndpoint();

        JSONObject terminalObject = new JSONObject(Terminal.getInstance().serialize());
        PluginResult result = new PluginResult(PluginResult.Status.OK, terminalObject);
        result.setKeepCallback(false);
        callbackContext.sendPluginResult(result);

    }

    private void initializeTerminalFromCloud(String activationCode, String activationKey, CallbackContext callbackContext)  {

        Terminal.initializeFromCloud(activationCode, activationKey, new TaskListener() {
            @Override
            public void onTaskComplete() {
                AnyPayCordova.this.endpoint = Terminal.getInstance().getEndpoint();

                try {
                    JSONObject terminalObject = new JSONObject(Terminal.getInstance().serialize());
                    PluginResult result = new PluginResult(PluginResult.Status.OK, terminalObject);
                    result.setKeepCallback(false);
                    callbackContext.sendPluginResult(result);
                } catch (Exception e) {
                    callbackContext.success();
                }

            }

            @Override
            public void onTaskFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });

    }

    private void authenticateTerminal(JSONObject configJSON, final CallbackContext callbackContext) {

        AuthenticationListener authenticationListener = new AuthenticationListener() {
            @Override
            public void onAuthenticationComplete() {
                Terminal.getInstance().getConfiguration().setProperty("endpoint", endpoint);
                Terminal.getInstance().saveState();
                callbackContext.success();
            }

            @Override
            public void onAuthenticationFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        };

        try {
            if (endpoint instanceof WorldnetEndpoint) {
                WorldnetEndpoint we = ((WorldnetEndpoint)endpoint);

                if (configJSON.has("worldnetSecret"))
                    we.setWorldnetSecret(configJSON.getString("worldnetSecret"));

                if (configJSON.has("worldnetTerminalID"))
                    we.setWorldnetTerminalID(configJSON.getString("worldnetTerminalID"));

                if (configJSON.has("gatewayUrl"))
                    we.setGatewayUrl(configJSON.getString("gatewayUrl"));

                we.authenticate(authenticationListener);
            }
            else if (endpoint instanceof PropayEndpoint) {
                PropayEndpoint pe = ((PropayEndpoint)endpoint);

                if (configJSON.has("x509Cert"))
                    pe.setX509Cert(configJSON.getString("x509Cert"));

                if (configJSON.has("certStr"))
                    pe.setCertStr(configJSON.getString("certStr"));

                if (configJSON.has("xmlApiBaseUrl"))
                    pe.setXmlApiBaseUrl(configJSON.getString("xmlApiBaseUrl"));

                if (configJSON.has("jsonApiBaseUrl"))
                    pe.setJsonApiBaseUrl(configJSON.getString("jsonApiBaseUrl"));

                if (configJSON.has("accountNum"))
                    pe.setAccountNum(configJSON.getString("accountNum"));

                if (configJSON.has("terminalId"))
                    pe.setTerminalId(configJSON.getString("terminalId"));

                pe.authenticate(authenticationListener);
            }
            else if (endpoint instanceof PriorityPaymentsEndpoint) {
                PriorityPaymentsEndpoint ppse = ((PriorityPaymentsEndpoint)endpoint);

                if (configJSON.has("username"))
                    ppse.setUsername(configJSON.getString("username"));

                if (configJSON.has("password"))
                    ppse.setPassword(configJSON.getString("password"));

                if (configJSON.has("consumerKey"))
                    ppse.setConsumerKey(configJSON.getString("consumerKey"));

                if (configJSON.has("secret"))
                    ppse.setSecret(configJSON.getString("secret"));

                if (configJSON.has("gatewayUrl"))
                    ppse.setUrl(configJSON.getString("gatewayUrl"));

                if (configJSON.has("merchantId"))
                    ppse.setMerchantId(configJSON.getString("merchantId"));

                ppse.authenticate(authenticationListener);
            }
        }
        catch (Exception e) {

        }
    }

    private void applyLoggingConfiguration(String loggerType, JSONObject configJSON, CallbackContext callbackContext) throws JSONException {

//         LogConfigurationProperties lp = new LogConfigurationProperties();
        LogConfigurationProperties lp = LogStream.getLogger(loggerType).getConfiguration();

        if (configJSON.has("logLevel"))
            lp.logLevel = configJSON.getString("logLevel");

        if (configJSON.has("remoteLoggingEnabled"))
            lp.remoteLoggingEnabled = configJSON.getBoolean("remoteLoggingEnabled");

        if (configJSON.has("realtimeLoggingEnabled"))
            lp.realtimeLoggingEnabled = configJSON.getBoolean("realtimeLoggingEnabled");

        if (configJSON.has("logToFile"))
            lp.logToFile = configJSON.getBoolean("logToFile");

        if (configJSON.has("logToConsole"))
            lp.logToConsole = configJSON.getBoolean("logToConsole");

        if (configJSON.has("batchingInterval"))
            lp.batchingInterval = configJSON.getInt("batchingInterval");

//         if (configJSON.has("streamLogTo"))
//             lp.streamLogTo = configJSON.getString("streamLogTo");

        LogStream.getLogger(loggerType).applyConfiguration(lp);

        callbackContext.success();
    }

    private void disconnectReader() {
        cardReaderController.disconnectReader();
    }

    private void subscribeOnCardReaderConnected(final CallbackContext callbackContext) {
        cardReaderConnectedListener = new GenericEventListenerWithParam<CardReader>() {
            @Override
            public void onEvent(final CardReader deviceInfo) {

                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        availableReaders = null;

                        if ( deviceInfo == null ) {
                            PluginResult result = new PluginResult(PluginResult.Status.OK);
                            result.setKeepCallback(true);
                            readerConnectedCallbackContext.sendPluginResult(result);
                        }
                        else {
                            try {
                                PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(deviceInfo.toString()));
                                result.setKeepCallback(true);
                                readerConnectedCallbackContext.sendPluginResult(result);
                            }
                            catch (Exception e) {
                                PluginResult result = new PluginResult(PluginResult.Status.OK);
                                result.setKeepCallback(true);
                                readerConnectedCallbackContext.sendPluginResult(result);
                            }
                        }
                    }
                });

            }
        };

        cardReaderController.subscribeOnCardReaderConnected(cardReaderConnectedListener);
    }

    private void subscribeOnCardReaderDisconnected(final CallbackContext callbackContext) {
        cardReaderDisconnectedListener = new GenericEventListener() {
            @Override
            public void onEvent() {

                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        availableReaders = null;

                        PluginResult result = new PluginResult(PluginResult.Status.OK, "Reader Disconnected");
                        result.setKeepCallback(true);
                        readerDisconnectedCallbackContext.sendPluginResult(result);
                    }
                });

            }
        };

        cardReaderController.subscribeOnCardReaderDisconnected(cardReaderDisconnectedListener);
    }

    private void subscribeOnCardReaderConnectFailed(final CallbackContext callbackContext) {
        cardReaderConnectionFailureListener = new MeaningfulErrorListener() {
            @Override
            public void onError(final MeaningfulError meaningfulError) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {

                        availableReaders = null;
//                        String message = "\nDevice connect failed: " + meaningfulError.toString();

                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, meaningfulError.toJSONObject());
                        result.setKeepCallback(true);
                        readerConnectionFailedCallbackContext.sendPluginResult(result);
                    }
                });
            }
        };

        cardReaderController.subscribeOnCardReaderConnectFailed(cardReaderConnectionFailureListener);
    }

    private void subscribeOnCardReaderError(final CallbackContext callbackContext) {
        cardReaderErrorListener = new MeaningfulErrorListener() {
            @Override
            public void onError(final MeaningfulError meaningfulError) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {

//                        String message = "\nDevice Error: " + meaningfulError.toString();

                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, meaningfulError.toJSONObject());
                        result.setKeepCallback(true);
                        readerConnectionErrorCallbackContext.sendPluginResult(result);
                    }
                });
            }
        };

        cardReaderController.subscribeOnCardReaderError(cardReaderErrorListener);
    }

    private void unsubscribeCardReaderCallbacks(final CallbackContext callbackContext) {
        cardReaderController.unsubscribeOnCardReaderConnected(cardReaderConnectedListener);
        cardReaderController.unsubscribeOnCardReaderConnectFailed(cardReaderConnectionFailureListener);
        cardReaderController.unsubscribeOnCardReaderDisconnected(cardReaderDisconnectedListener);
        cardReaderController.unsubscribeOnCardReaderError(cardReaderErrorListener);
        callbackContext.success();
    }

    private void connectToBluetoothReader(boolean selectOnMultipleReadersAvailable, final CallbackContext callbackContext) {

        if (selectOnMultipleReadersAvailable) {
            cardReaderController.connectBluetooth(new MultipleBluetoothDevicesFoundListener() {
                @Override
                public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {
                    availableReaders = matchingDevices;
                    ArrayList<String> readerIDs = new ArrayList<>();

                    for (BluetoothDevice d : matchingDevices) {
                        readerIDs.add(d.getName());
                    }

                    PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONArray(readerIDs));
                    result.setKeepCallback(false);
                    callbackContext.sendPluginResult(result);
                }
            });
        }
        else {
            cardReaderController.connectBluetooth(null);
        }
    }

    private void connectToBluetoothReaderWithID(String id) {

        for (BluetoothDevice d : availableReaders) {
            if (d.getName().equalsIgnoreCase(id)) {
                cardReaderController.connectSpecificBluetoothDevice(d);
                break;
            }
        }

        availableReaders = null;
    }

    private void connectAudioReader() {
        cardReaderController.connectAudioJack();
    }


    private void subscribeToCardReaderEvent(final CallbackContext callbackContext) {
        cardReaderEventCallbackContext = callbackContext;
    }

    private void startEMVSale(final AnyPayTransaction transaction, final CallbackContext callbackContext) {

        if ( !CardReaderController.isCardReaderConnected() ) {
            try {
                MeaningfulError err = new MeaningfulError();
                err.message = "No Card Reader Connected";
                err.detail = "Please connect a card reader";
                JSONObject transactionObject = new JSONObject(transaction.serialize());
                transactionObject.put("approved", transaction.isApproved());
                transactionObject.put("internalId", transaction.getInternalId());
                transactionObject.put("transactionError", err.toJSONObject());

                transactionCallbackContext.error(transactionObject);
            } catch (Exception e) {
                transactionCallbackContext.error(new JSONObject());
            }

            return;
        }

        transaction.useCardReader(CardReaderController.getConnectedReader()); // either instance, or

        refTransaction = transaction;

        if (signatureRequiredCallbackContext != null) {
            transaction.setOnSignatureRequiredListener(new GenericEventListener() {
                @Override
                public void onEvent() {
                    signatureRequiredCallbackContext.success();
                }
            });
        }

        transaction.execute(new CardTransactionListener() {
            @Override
            public void onCardReaderEvent(MeaningfulMessage event) {

                PluginResult result = new PluginResult(PluginResult.Status.OK, event.message);
                result.setKeepCallback(true);
                cardReaderEventCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onTransactionCompleted() {

                try {
                    JSONObject transactionObject = new JSONObject(transaction.serialize());
                    transactionObject.put("approved", transaction.isApproved());
                    transactionObject.put("internalId", transaction.getInternalId());

                    PluginResult result = new PluginResult(PluginResult.Status.OK, transactionObject);
                    result.setKeepCallback(false);
                    transactionCallbackContext.sendPluginResult(result);
                }
                catch (Exception e) {
                    transactionCallbackContext.success();
                }


            }

            @Override
            public void onTransactionFailed(MeaningfulError meaningfulError) {
                try {
                    JSONObject errorObject = meaningfulError.toJSONObject();
                    JSONObject transactionObject = new JSONObject(transaction.serialize());
                    transactionObject.put("approved", transaction.isApproved());
                    transactionObject.put("internalId", transaction.getInternalId());
                    transactionObject.put("transactionError", errorObject);

                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, transactionObject);
                    result.setKeepCallback(false);
                    transactionCallbackContext.sendPluginResult(result);
                } catch (Exception e) {
                    transactionCallbackContext.error(new JSONObject());
                }

            }
        });

    }

    private void startKeyedTransaction(AnyPayTransaction transaction, final CallbackContext callbackContext) {

        if ((transaction.getTransactionType() == TransactionType.VOID) || (transaction.getTransactionType() == TransactionType.REFUND) || (transaction.getTransactionType() == TransactionType.REVERSEAUTH)) {
            if (transaction.getExternalId() != null) {
                if (transaction.getExternalId().length() > 0) {
                    transaction = (AnyPayTransaction) transaction.createReversal();
                    transaction.setEndpoint(endpoint);
                }
            }
        }

        final AnyPayTransaction t = transaction;

        refTransaction = t;

        t.execute(new TransactionListener() {

            @Override
            public void onTransactionCompleted() {


                try {
                    JSONObject transactionObject = new JSONObject(t.serialize());
                    transactionObject.put("approved", t.isApproved());
                    transactionObject.put("internalId", t.getInternalId());

                    PluginResult result = new PluginResult(PluginResult.Status.OK, transactionObject);
                    result.setKeepCallback(false);
                    transactionCallbackContext.sendPluginResult(result);
                }
                catch (Exception e) {
                    transactionCallbackContext.success();
                }

            }

            @Override
            public void onTransactionFailed(MeaningfulError meaningfulError) {

                try {
                    JSONObject errorObject = meaningfulError.toJSONObject();
                    JSONObject transactionObject = new JSONObject(t.serialize());
                    transactionObject.put("approved", t.isApproved());
                    transactionObject.put("internalId", t.getInternalId());
                    transactionObject.put("transactionError", errorObject);

                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, transactionObject);
                    result.setKeepCallback(false);
                    transactionCallbackContext.sendPluginResult(result);
                } catch (Exception e) {
                    transactionCallbackContext.error(new JSONObject());
                }

            }
        });

    }

    private AnyPayTransaction createTransactionObject(JSONObject transactionJSON) {
//        AnyPayTransaction transaction = new AnyPayTransaction();




//        transaction.setEndpoint(endpoint);

        try {
            String tipIfPresent = null;
            if (!transactionJSON.isNull("tip")) {
                if ((transactionJSON.get("tip") instanceof String) || (transactionJSON.get("tip") instanceof Integer) || (transactionJSON.get("tip") instanceof Float) || (transactionJSON.get("tip") instanceof Double))
                    tipIfPresent = transactionJSON.getString("tip");
                transactionJSON.remove("tip");
            }

            GsonBuilder gsonBuilder = new GsonBuilder()
                    .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
            gsonBuilder.registerTypeAdapterFactory(PostProcessingTypeAdapterFactory.get());

            //.registerTypeAdapter(WorldnetEndpointConfiguration.class, new AbstractClassTypeAdapter())
            Gson gson = gsonBuilder.create();
            AnyPayTransaction transaction = gson.fromJson(transactionJSON.toString(), AnyPayTransaction.class);

//            if (!transactionJSON.isNull("type"))
//                transaction.setTransactionType(TransactionType.fromValue(transactionJSON.getString("type")));
//
//            if (!transactionJSON.isNull("totalAmount"))
//                transaction.setTotalAmount(new Amount(transactionJSON.getString("totalAmount")));
//
//            if (!transactionJSON.isNull("subtotal"))
//                transaction.setSubtotal(new Amount(transactionJSON.getString("subtotal")));

            if (!transactionJSON.isNull("tax"))
                transaction.addTax(new TaxLineItem("Tax", transactionJSON.getString("tax")));

            if (!TextUtils.isEmpty(tipIfPresent))
                transaction.setTip(new TipLineItem(tipIfPresent));

//            if (!transactionJSON.isNull("currency"))
//                transaction.setCurrency(transactionJSON.getString("currency"));
//
//            if (!transactionJSON.isNull("cardExpiryMonth"))
//                transaction.setCardExpiryMonth(transactionJSON.getString("cardExpiryMonth"));
//
//            if (!transactionJSON.isNull("cardExpiryYear"))
//                transaction.setCardExpiryYear(transactionJSON.getString("cardExpiryYear"));
//
//            if (!transactionJSON.isNull("cardNumber"))
//                transaction.setCardNumber(transactionJSON.getString("cardNumber"));
//
//            if (!transactionJSON.isNull("CVV2"))
//                transaction.setCVV2(transactionJSON.getString("CVV2"));
//
//            if (!transactionJSON.isNull("maskedPAN"))
//                transaction.setMaskedPAN(transactionJSON.getString("maskedPAN"));
//
//            if (!transactionJSON.isNull("address"))
//                transaction.setAddress(transactionJSON.getString("address"));
//
//            if (!transactionJSON.isNull("cardholderName"))
//                transaction.setCardholderName(transactionJSON.getString("cardholderName"));
//
//            if (!transactionJSON.isNull("postalCode"))
//                transaction.setPostalCode(transactionJSON.getString("postalCode"));
//
//            if (!transactionJSON.isNull("internalId"))
//                transaction.setInternalId(transactionJSON.getString("internalId"));
//
//            if (!transactionJSON.isNull("externalId"))
//                transaction.setExternalId(transactionJSON.getString("externalId"));
//
//            if (!transactionJSON.isNull("refTransactionID"))
//                transaction.setRefTransactionId(transactionJSON.getString("refTransactionID"));
//
//            if (!transactionJSON.isNull("invoiceNumber"))
//                transaction.setInvoiceNumber(transactionJSON.getString("invoiceNumber"));

            if (!transactionJSON.isNull("customerEmail")) {
                CustomerDetails customerDetails = new CustomerDetails();
                customerDetails.setEmailAddress(transactionJSON.getString("customerEmail"));
                transaction.setCustomerDetails(customerDetails);
            }

//            if (!transactionJSON.isNull("notes"))
//                transaction.setNotes(transactionJSON.getString("notes"));
//
//            if (!transactionJSON.isNull("orderId"))
//                transaction.setOrderId(transactionJSON.getString("orderId"));
//
            if (!transactionJSON.isNull("customFields")) {
                JSONObject jObject = transactionJSON.getJSONObject("customFields");
                Iterator<String> keys = jObject.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    if (!jObject.isNull(key)) {
                        transaction.addCustomField(key, jObject.get(key));
                    }
                }
            }

            if (!transactionJSON.isNull("cardInterfaceModes")) {
                JSONArray arr = transactionJSON.getJSONArray("cardInterfaceModes");
                List<String> list = new ArrayList<String>();
                for (int i = 0; i < arr.length();list.add(arr.getString(i++)));

                StringBuilder builder = new StringBuilder();

                for (String string : list) {
                    if (builder.length() > 0) {
                        builder.append(" ");
                    }

                    builder.append(string);
                }

                if (CardReaderController.getConnectedReader() != null)
                    CardReaderController.getConnectedReader().setEnabledInterfaces(getEnumsetForEntryModes(builder.toString()));
            }

            return transaction;
        }
        catch (JSONException ex) {

        }

        return null;
    }

    private static EnumSet<CardInterface> getEnumsetForEntryModes(String entrymodes) {
        EnumSet<CardInterface> enabledEntryModes = EnumSet.noneOf(CardInterface.class);

        entrymodes = entrymodes.replace("DIP", CardInterface.INSERT.toString());
        entrymodes = entrymodes.replace("NFC", CardInterface.TAP.toString());
        entrymodes = entrymodes.replace("KEYED", CardInterface.PINPAD.toString());

        if (entrymodes.contains(CardInterface.SWIPE.toString()))
            enabledEntryModes.add(CardInterface.SWIPE);
        if (entrymodes.contains(CardInterface.INSERT.toString()))
            enabledEntryModes.add(CardInterface.INSERT);
        if (entrymodes.contains(CardInterface.TAP.toString()))
            enabledEntryModes.add(CardInterface.TAP);
        if (entrymodes.contains(CardInterface.PINPAD.toString()))
            enabledEntryModes.add(CardInterface.PINPAD);
        if (entrymodes.contains(CardInterface.OCR.toString()))
            enabledEntryModes.add(CardInterface.OCR);

        return enabledEntryModes;
    }

    private void setOnSignatureRequired(final CallbackContext callbackContext) {
        signatureRequiredCallbackContext = callbackContext;
    }

    private void proceed(ArrayList<ArrayList> signaturePoints, CallbackContext callbackContext) {
        refTransaction.setSignature(pointsToSignatureArray(signaturePoints));
        refTransaction.proceed();
    }

    private void adjustTip(String rate, final CallbackContext callbackContext) {
        if (endpoint instanceof WorldnetEndpoint) {
            ((WorldnetEndpoint)this.endpoint).submitTipAdjustment(refTransaction, new TipLineItem(rate), new RequestListener() {
                @Override
                public void onRequestComplete(Object o) {
                    callbackContext.success();
                }

                @Override
                public void onRequestFailed(MeaningfulError meaningfulError) {
                    callbackContext.error(meaningfulError.toJSONObject());
                }
            });
        }
    }

    private void updateSignature(ArrayList<ArrayList> signaturePoints, final CallbackContext callbackContext) {
        if (signaturePoints.size() > 0) {
            refTransaction.setSignature(pointsToSignatureArray(signaturePoints));
            refTransaction.update(new TransactionListener() {
                @Override
                public void onTransactionCompleted() {
                    callbackContext.success();
                }

                @Override
                public void onTransactionFailed(MeaningfulError meaningfulError) {
                    callbackContext.error(meaningfulError.toJSONObject());
                }
            });
        }
        else {
            callbackContext.error("Signature array is empty");
        }
    }

    private void fetchTransactions(int page, String orderID, final CallbackContext callbackContext) {
        endpoint.fetchTransactions(page, 100, orderID, null, null, new RequestListener<Object>() {
            @Override
            public void onRequestComplete(Object o) {

                if (endpoint instanceof WorldnetEndpoint) {
                    JSONArray a = new JSONArray();
                    for (Object t : (ArrayList<Object>) o) {
                        a.put(((AnyPayTransaction)t).serialize());
                    }

                    callbackContext.success(a);
                }
                else if (endpoint instanceof PropayEndpoint) {
                    JSONArray a = new JSONArray();
                    for (JSONObject t : (ArrayList<JSONObject>) o) {
                        a.put(t);
                    }
                    callbackContext.success(a);
                }
                else if (endpoint instanceof PriorityPaymentsEndpoint) {
                    callbackContext.success((JSONArray) o);
                }
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private void sendReceipt(String toEmail, String toPhone, final CallbackContext callbackContext) {
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setEmailAddress(toEmail);
        customerDetails.setPhoneNumber(toPhone);
        refTransaction.setCustomerDetails(customerDetails);
        refTransaction.update(new TransactionListener() {
            @Override
            public void onTransactionCompleted() {
                callbackContext.success();
            }

            @Override
            public void onTransactionFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private Signature pointsToSignatureArray(ArrayList<ArrayList> sPoints) {
        try {
            Signature signature = new Signature();
            ArrayList<DrawPoint> signaturePoints = new ArrayList<DrawPoint>();

            for (ArrayList<JSONObject> pointsDict:sPoints) {
                for (int i=0; i<pointsDict.size(); i++) {
                    DrawPoint point = new DrawPoint();

                    float x = ((Double)pointsDict.get(i).getDouble("x")).floatValue();
                    float y = ((Double)pointsDict.get(i).getDouble("y")).floatValue();

                    if (i == 0)
                        point.setStart(x, y);
                    else if (i == pointsDict.size() - 1)
                        point.setEnd(x, y);
                    else
                        point.setMove(x, y);

                    signaturePoints.add(point);
                }
            }

            signature.setSignaturePoints(signaturePoints);
            return signature;
        }
        catch (Exception e) {

        }

        return null;
    }

    private ArrayList jsonArraytoArrayList(JSONArray jArray) {
        try {
            ArrayList listdata = new ArrayList();
            if (jArray != null) {
                for (int i=0; i<jArray.length(); i++) {

                    Object a = jArray.get(i);
                    if (a instanceof JSONArray) {
                        a = jsonArraytoArrayList((JSONArray) a);
                    }

                    listdata.add(a);
                }
            }

            return listdata;
        }
        catch (Exception e) {

        }

        return null;
    }

    private Endpoint mappedEndpoint(JSONObject configJSON) {
        if (!configJSON.has("provider"))
            return null;

        Endpoint endpoint = null;

        try {
            String provider = configJSON.getString("provider");

            if (provider.equalsIgnoreCase("worldnet")) {
                WorldnetEndpoint we = new WorldnetEndpoint();

                if (configJSON.has("worldnetSecret"))
                    we.setWorldnetSecret(configJSON.getString("worldnetSecret"));

                if (configJSON.has("worldnetTerminalID"))
                    we.setWorldnetTerminalID(configJSON.getString("worldnetTerminalID"));

                if (configJSON.has("gatewayUrl"))
                    we.setGatewayUrl(configJSON.getString("gatewayUrl"));

                endpoint = we;
            }
            else if (provider.equalsIgnoreCase("propay")) {
                PropayEndpoint pe = new PropayEndpoint();

                if (configJSON.has("x509Cert"))
                    pe.setX509Cert(configJSON.getString("x509Cert"));

                if (configJSON.has("certStr"))
                    pe.setCertStr(configJSON.getString("certStr"));

                if (configJSON.has("xmlApiBaseUrl"))
                    pe.setXmlApiBaseUrl(configJSON.getString("xmlApiBaseUrl"));

                if (configJSON.has("jsonApiBaseUrl"))
                    pe.setJsonApiBaseUrl(configJSON.getString("jsonApiBaseUrl"));

                if (configJSON.has("accountNum"))
                    pe.setAccountNum(configJSON.getString("accountNum"));

                if (configJSON.has("terminalId"))
                    pe.setTerminalId(configJSON.getString("terminalId"));

                endpoint = pe;

            }
            else if (provider.equalsIgnoreCase("pps")) {
                PriorityPaymentsEndpoint ppse = new PriorityPaymentsEndpoint();

                if (configJSON.has("username"))
                    ppse.setUsername(configJSON.getString("username"));

                if (configJSON.has("password"))
                    ppse.setPassword(configJSON.getString("password"));

                if (configJSON.has("consumerKey"))
                    ppse.setConsumerKey(configJSON.getString("consumerKey"));

                if (configJSON.has("secret"))
                    ppse.setSecret(configJSON.getString("secret"));

                if (configJSON.has("gatewayUrl"))
                    ppse.setUrl(configJSON.getString("gatewayUrl"));

                if (configJSON.has("merchantId"))
                    ppse.setMerchantId(configJSON.getString("merchantId"));

                endpoint = ppse;
            }
        }
        catch (Exception e) {

        }

        return endpoint;
    }

    private void setCloudFlavor(String flavor, final CallbackContext callbackContext) {
        CloudAPI.setFlavor(flavor);
        callbackContext.success();
    }

    private void pollForMessage(final CallbackContext callbackContext) {
        CloudAPI.pollForMessage(Terminal.getInstance(), new RequestListener<CloudPosTerminalMessage>() {
            @Override
            public void onRequestComplete(CloudPosTerminalMessage cloudPosTerminalMessage) {
                try {
                    PluginResult result = null;

                    if (cloudPosTerminalMessage == null) {
                        // Not found state

                        MeaningfulError err = new MeaningfulError();
                        err.errorCode = "404";
                        err.message = "Not Found";
                        err.detail = "No Message Available";
                        result = new PluginResult(PluginResult.Status.ERROR, err.toJSONObject());
                    }
                    else {
                        result = new PluginResult(PluginResult.Status.OK, new JSONObject(cloudPosTerminalMessage.toJson()));
                    }

                    result.setKeepCallback(false);
                    callbackContext.sendPluginResult(result);
                }
                catch (Exception e) {
                    callbackContext.error("Unable to parse Message object");
                }
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private void startReceivingTransactions(final CallbackContext callbackContext) {
        this.terminalMessageCallbackContext = callbackContext;
        subscribeMessageListeners();
        CloudPosTerminalMessageQueue.getInstance().start();
    }

    private void stopReceivingTransactions() {
        unsubscribeMessageListeners();
        this.terminalMessageCallbackContext = null;
        CloudPosTerminalMessageQueue.getInstance().stop();
        CloudPosTerminalMessageQueue.getInstance().unsubscribeAllListeners();
    }

    private void subscribeToConnectionState(final CallbackContext callbackContext) {
        terminalConnectionStateCallbackContext = callbackContext;

        OnTerminalConnectionStatusChanged = new GenericEventListenerWithParam<CloudPosTerminalConnectionStatus>() {
            @Override
            public void onEvent(final CloudPosTerminalConnectionStatus status) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, status.toString());
                result.setKeepCallback(true);
                terminalConnectionStateCallbackContext.sendPluginResult(result);
            }
        };

        CloudPosTerminalMessageQueue.getInstance().OnConnectionStatusChanged = OnTerminalConnectionStatusChanged;
    }

    private void unsubscribeToConnectionState() {
        OnTerminalConnectionStatusChanged = null;
        terminalConnectionStateCallbackContext = null;
        CloudPosTerminalMessageQueue.getInstance().OnConnectionStatusChanged = null;
    }

    public void subscribeMessageListeners() {
        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("MESSAGE", incomingMessageListener);
        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("NEW_TRANSACTION", incomingTransactionMessageListener);
        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CANCEL_TRANSACTION", incomingTransactionMessageListener);
        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CONFIG_CHANGED", incomingConfigUpdateMessageListener);
    }

    public void unsubscribeMessageListeners() {
        CloudPosTerminalMessageQueue.getInstance().unsubscribeToMessagesOfType("MESSAGE", incomingMessageListener);
        CloudPosTerminalMessageQueue.getInstance().unsubscribeToMessagesOfType("NEW_TRANSACTION", incomingTransactionMessageListener);
        CloudPosTerminalMessageQueue.getInstance().unsubscribeToMessagesOfType("CANCEL_TRANSACTION", incomingTransactionMessageListener);
        CloudPosTerminalMessageQueue.getInstance().unsubscribeToMessagesOfType("CONFIG_CHANGED", incomingConfigUpdateMessageListener);
    }

    GenericEventListenerWithParam<CloudPosTerminalMessage> incomingMessageListener = new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
        @Override
        public void onEvent(final CloudPosTerminalMessage message) {
            if (message != null) {
                message.accept();
                invokeTerminalMessageCallback(message);
            }
        }
    };

    GenericEventListenerWithParam<CloudPosTerminalMessage> incomingConfigUpdateMessageListener = new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
        @Override
        public void onEvent(final CloudPosTerminalMessage message) {

            if (message != null) {
                if (message.terminalUUID.equalsIgnoreCase(Terminal.getInstance().getUuid())) {
                    Terminal.getInstance().overwriteConfiguration(message.terminal);
                    message.accept();
                }
                else {
                    message.reason = "Terminal UUID mismatch";
                    message.reject();
                }
            }
        }
    };

    GenericEventListenerWithParam<CloudPosTerminalMessage> incomingTransactionMessageListener = new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
        @Override
        public void onEvent(CloudPosTerminalMessage message) {
            if (message != null) {
                invokeTerminalMessageCallback(message);
            }
        }
    };

    private void invokeTerminalMessageCallback(CloudPosTerminalMessage message) {
        if (terminalMessageCallbackContext == null)
            return;

        try {
            PluginResult result = null;

            if (message == null) {
                // Not found state

                MeaningfulError err = new MeaningfulError();
                err.errorCode = "404";
                err.message = "Not Found";
                err.detail = "No Message Available";
                result = new PluginResult(PluginResult.Status.ERROR, err.toJSONObject());
            }
            else {
                result = new PluginResult(PluginResult.Status.OK, new JSONObject(message.toJson()));
            }

            result.setKeepCallback(true);
            terminalMessageCallbackContext.sendPluginResult(result);
        }
        catch (Exception e) {
            MeaningfulError err = new MeaningfulError();
            err.errorCode = "1001";
            err.message = "Unable to parse Message object";
            err.detail = e.getLocalizedMessage();

            terminalMessageCallbackContext.error(err.toJSONObject());
        }
    }

    private void acceptMessage(JSONObject json, final CallbackContext callbackContext) {
        CloudPosTerminalMessage message = deserializeMessage(json);
        message.accept();
        callbackContext.success();
    }

    private void rejectMessage(JSONObject json, final CallbackContext callbackContext) {
        CloudPosTerminalMessage message = deserializeMessage(json);
        message.reject();
        callbackContext.success();
    }

    private void finishedMessage(JSONObject json, final CallbackContext callbackContext) {
        CloudPosTerminalMessage message = deserializeMessage(json);
        message.finished();
        callbackContext.success();
    }

    private void updateTransaction(JSONObject json, final CallbackContext callbackContext) {
        AnyPayTransaction transaction = createTransactionObject(json);
        CloudAPI.updateCloudTransaction(Terminal.getInstance(), transaction, new RequestListener<Void>() {

            @Override
            public void onRequestComplete(Void response) {
                callbackContext.success();
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                Logger.logException(meaningfulError);
                //startPolling();
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private void acceptTransaction(JSONObject json, final CallbackContext callbackContext) {
        AnyPayTransaction transaction = createTransactionObject(json);
        if (transaction.getStatus() == TransactionStatus.QUEUED) {
            transaction.setStatus(TransactionStatus.PROCESSING);
        }

        CloudAPI.acceptTransaction(Terminal.getInstance(), transaction, new RequestListener<Void>() {

            @Override
            public void onRequestComplete(Void response) {
                callbackContext.success();
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                Logger.logException(meaningfulError);
                //startPolling();
                callbackContext.error(meaningfulError.toJSONObject());
            }
        });
    }

    private CloudPosTerminalMessage deserializeMessage(JSONObject json) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
        gsonBuilder.registerTypeAdapterFactory(PostProcessingTypeAdapterFactory.get());

        Gson gson = gsonBuilder.create();
        CloudPosTerminalMessage message = gson.fromJson(json.toString(), CloudPosTerminalMessage.class);
        return message;
    }

    private void cancelTransaction(CallbackContext callbackContext) {
        if (((Boolean)refTransaction.getCustomField("ReaderProcessingStarted", false))) {
            MeaningfulError err = new MeaningfulError();
            err.message = "Transaction in non cancellable state";
            err.detail = "Transaction could not be cancelled";
            callbackContext.error(err.toJSONObject());
        } else {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        AnyPayCordova.this.refTransaction.cancel();
                    }
                    catch (Exception e) {

                    }
                }
            });
            callbackContext.success("Cancellation Started");
        }

    }
}
