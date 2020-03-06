package com.wrobins.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;


public class MsalPlugin extends CordovaPlugin {
    private Activity activity;
    private Context context;
    private CallbackContext callbackContext;
    private ISingleAccountPublicClientApplication appSingleClient;
    private IMultipleAccountPublicClientApplication appMultipleClient;

    private String clientId;
    private String tenantId;
    private String keyHash;
    private String accountMode;

    private static final String SIGN_IN_SILENT = "signInSilent";
    private static final String SIGN_IN_INTERACTIVE = "signInInteractive";
    private static final String SIGN_OUT = "signOut";
    private static final String MSAL_INIT = "msalInit";
    private static final String GET_ACCOUNTS = "getAccounts";

    private static final String SINGLE_ACCOUNT = "SINGLE";
    private static final String MULTIPLE_ACCOUNTS = "MULTIPLE";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        context = webView.getContext();

        clientId = this.preferences.getString("clientId","");
        tenantId = this.preferences.getString("tenantId","common");
        keyHash = this.preferences.getString("keyHash","");


    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        try {
            if (MSAL_INIT.equals(action)) {
                this.msalInit(args.getJSONObject(0));
            }
            if (SIGN_IN_SILENT.equals(action)) {
                this.signinUserSilent();
            }
            if (SIGN_OUT.equals(action)) {
                this.signOut();
            }
            if (SIGN_IN_INTERACTIVE.equals(action)) {
                this.signinUserInteractive();
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }

        return true;
    }

    private void msalInit(final JSONObject options) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String keyHashUrlFriendly = "";
                    try {
                        keyHashUrlFriendly = URLEncoder.encode(MsalPlugin.this.keyHash, "UTF-8");
                    } catch(UnsupportedEncodingException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    }
                    StringBuilder authorities = new StringBuilder("    \"authorities\" : [\n");
                    String data = "";
                    try {
                        JSONArray authoritiesList = options.getJSONArray("authorities");
                        for (int i = 0; i < authoritiesList.length(); ++i) {
                            JSONObject authority = authoritiesList.getJSONObject(i);
                            authorities.append("      {\n");
                            authorities.append("        \"type\": \"" + authority.getString("type") + "\",\n");
                            authorities.append("        \"audience\": {\n");
                            authorities.append("          \"type\": \"" + authority.getString("audience") + "\",\n");
                            authorities.append("          \"tenant_id\": \"" + MsalPlugin.this.tenantId + "\"\n");
                            authorities.append("        }\n");
                            if (authority.has("authorityUrl")) {
                                authorities.append("          \"authority_url\": \"" + authority.getString("authorityUrl") + "\"\n");
                            }
                            if (authority.has("default")) {
                                authorities.append("          \"default\": " + authority.getBoolean("default") + "\n");
                            }
                            authorities.append("      }\n");
                        }
                        authorities.append("    ]\n");
                        data = "{\n" +
                                "    \"client_id\" : \"" + MsalPlugin.this.clientId + "\",\n" +
                                "    \"account_mode\": \"" + options.getString("accountMode") + "\",\n" +
                                // TODO: Make WEBVIEW dynamic after OutSystems branch: options.getString("authorizationUserAgent")
                                "    \"authorization_user_agent\" : \"WEBVIEW\",\n" +
                                "    \"redirect_uri\" : \"msauth://" + MsalPlugin.this.activity.getApplicationContext().getPackageName() + "/" + keyHashUrlFriendly + "\",\n" +
                                "    \"multiple_clouds_supported\": " + options.getBoolean("multipleCloudsSupported") + ",\n" +
                                "    \"broker_redirect_uri_registered\": " + options.getBoolean("brokerRedirectUri") + ",\n" +
                                authorities.toString() +
                                "  }";
                        File config = createConfigFile(data);
                        if (options.getString("accountMode").equals(SINGLE_ACCOUNT)) {
                            MsalPlugin.this.appSingleClient = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                            MsalPlugin.this.accountMode = SINGLE_ACCOUNT;
                        } else {
                            MsalPlugin.this.appMultipleClient = MultipleAccountPublicClientApplication.createMultipleAccountPublicClientApplication(context, config);
                            MsalPlugin.this.accountMode = MULTIPLE_ACCOUNTS;
                        }
                        config.delete();
                    } catch (JSONException ignored) {}
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MsalException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void signinUserSilent() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] scopes = {"User.Read"};
                    String authority = MsalPlugin.this.appSingleClient.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
                    if(MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() == null) {
                        MsalPlugin.this.callbackContext.error("No account currently exists");
                    } else {
                        IAuthenticationResult silentAuthResult = MsalPlugin.this.appSingleClient.acquireTokenSilent(scopes, authority);
                        MsalPlugin.this.callbackContext.success(silentAuthResult.getAccessToken());
                    }
                } catch (InterruptedException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                } catch (MsalException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void signinUserInteractive() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] scopes = {"User.Read"};
                    MsalPlugin.this.appSingleClient.signIn(MsalPlugin.this.activity, "", scopes, new AuthenticationCallback() {
                        @Override
                        public void onCancel() {
                            MsalPlugin.this.callbackContext.error("Login cancelled.");
                        }

                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            MsalPlugin.this.callbackContext.success(authenticationResult.getAccessToken());
                        }

                        @Override
                        public void onError(MsalException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void signOut() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if(MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() != null) {
                        MsalPlugin.this.appSingleClient.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                            @Override
                            public void onSignOut() {
                                MsalPlugin.this.callbackContext.success();
                            }
                
                            @Override
                            public void onError(MsalException e) {
                                MsalPlugin.this.callbackContext.error(e.getMessage());
                            }
                        });
                    } else {
                        MsalPlugin.this.callbackContext.success();
                    }
                } catch (InterruptedException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                } catch (MsalException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private File createConfigFile(String data) {
        File config = new File(this.context.getFilesDir() + "auth_config.json");
        if (config.exists()) {
            return config;
        } else {
            try {
                FileWriter writer = new FileWriter(config, false);
                writer.write(data);
                writer.flush();
                writer.close();
                return config;
            } catch (IOException e) {
                MsalPlugin.this.callbackContext.error(e.getMessage());
            }
        }
        return config;
    }
}