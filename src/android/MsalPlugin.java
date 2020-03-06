package com.wrobins.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.json.JSONException;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;


public class MsalPlugin extends CordovaPlugin {
    private Activity activity;
    private Context context;
    private CallbackContext callbackContext;
    private ISingleAccountPublicClientApplication app;
    private boolean appCreated = false;

    private String clientId;
    private String tenantId;
    private String keyHash;

    private static final String SIGN_IN_SILENT = "signInSilent";
    private static final String SIGN_IN_INTERACTIVE = "signInInteractive";
    private static final String SIGN_OUT = "signOut";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        context = webView.getContext();

        clientId = this.preferences.getString("clientId","");
        tenantId = this.preferences.getString("tenantId","");
        keyHash = this.preferences.getString("keyHash","");

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File config = createConfigFile();
                    MsalPlugin.this.app = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                    config.delete();
                    MsalPlugin.this.appCreated = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MsalException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        try {
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

    private void signinUserSilent() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!MsalPlugin.this.appCreated) {
                        File config = createConfigFile();
                        MsalPlugin.this.app = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                        config.delete();
                        MsalPlugin.this.appCreated = true;
                    }
                    String[] scopes = {"User.Read"};
                    String authority = MsalPlugin.this.app.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
                    if(MsalPlugin.this.app.getCurrentAccount().getCurrentAccount() == null) {
                        MsalPlugin.this.callbackContext.error("No account currently exists");
                    } else {
                        IAuthenticationResult silentAuthResult = MsalPlugin.this.app.acquireTokenSilent(scopes, authority);
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
                    if (!MsalPlugin.this.appCreated) {
                        File config = createConfigFile();
                        MsalPlugin.this.app = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                        config.delete();
                        MsalPlugin.this.appCreated = true;
                    }
                    String[] scopes = {"User.Read"};
                    MsalPlugin.this.app.signIn(MsalPlugin.this.activity, "", scopes, new AuthenticationCallback() {
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
                } catch (InterruptedException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                } catch (MsalException e) {
                    MsalPlugin.this.callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void signOut() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!MsalPlugin.this.appCreated) {
                        File config = createConfigFile();
                        MsalPlugin.this.app = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                        config.delete();
                        MsalPlugin.this.appCreated = true;
                    }
                    String[] scopes = {"User.Read"};
                    if(MsalPlugin.this.app.getCurrentAccount().getCurrentAccount() != null) {
                        MsalPlugin.this.app.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
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

    private File createConfigFile() {
        String keyHashUrlFriendly = "";
        try {
            keyHashUrlFriendly = URLEncoder.encode(MsalPlugin.this.keyHash, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            MsalPlugin.this.callbackContext.error(e.getMessage());
        }
        String data = "{\n" +
        "    \"client_id\" : \"" + MsalPlugin.this.clientId + "\",\n" +
        "    \"account_mode\": \"SINGLE\",\n" +
        "    \"authorization_user_agent\" : \"WEBVIEW\",\n" +
        "    \"redirect_uri\" : \"msauth://" + MsalPlugin.this.activity.getApplicationContext().getPackageName() + "/" + keyHashUrlFriendly + "\",\n" +
        "    \"broker_redirect_uri_registered\": true,\n" +
        "    \"authorities\" : [\n" +
        "      {\n" +
        "        \"type\": \"AAD\",\n" +
        "        \"audience\": {\n" +
        "          \"type\": \"AzureADMyOrg\",\n" +
        "          \"tenant_id\": \"" + MsalPlugin.this.tenantId + "\"\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }";
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