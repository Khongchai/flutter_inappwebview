package com.pichillilorenzo.flutter_inappwebview;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.pichillilorenzo.flutter_inappwebview.types.ChannelDelegateImpl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class InAppWebViewStatic extends ChannelDelegateImpl {
  protected static final String LOG_TAG = "InAppWebViewStatic";
  public static final String METHOD_CHANNEL_NAME = "com.pichillilorenzo/flutter_inappwebview_static";
  
  @Nullable
  public InAppWebViewFlutterPlugin plugin;

  public InAppWebViewStatic(final InAppWebViewFlutterPlugin plugin) {
    super(new MethodChannel(plugin.messenger, METHOD_CHANNEL_NAME));
    this.plugin = plugin;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull final MethodChannel.Result result) {
    switch (call.method) {
      case "getDefaultUserAgent":
        result.success(WebSettings.getDefaultUserAgent(plugin.applicationContext));
        break;
      case "clearClientCertPreferences":
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          WebView.clearClientCertPreferences(new Runnable() {
            @Override
            public void run() {
              result.success(true);
            }
          });
        } else {
          result.success(false);
        }
        break;
      case "getSafeBrowsingPrivacyPolicyUrl":
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL)) {
          result.success(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString());
        } else
          result.success(null);
        break;
      case "setSafeBrowsingAllowlist":
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ALLOWLIST)) {
          Set<String> hosts = new HashSet<>((List<String>) call.argument("hosts"));
          WebViewCompat.setSafeBrowsingAllowlist(hosts, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
              result.success(value);
            }
          });
        }
        else if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_WHITELIST)) {
          List<String> hosts = (List<String>) call.argument("hosts");
          WebViewCompat.setSafeBrowsingWhitelist(hosts, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
              result.success(value);
            }
          });
        } else
          result.success(false);
        break;
      case "getCurrentWebViewPackage":
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && plugin != null && (plugin.activity != null || plugin.applicationContext != null)) {
          Context context = plugin.activity;
          if (context == null) {
            context = plugin.applicationContext;
          }
          result.success(convertWebViewPackageToMap(WebViewCompat.getCurrentWebViewPackage(context)));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          //with Android Lollipop (API 21) they started to update the WebView
          //as a separate APK with the PlayStore and they added the
          //getLoadedPackageInfo() method to the WebViewFactory class and this
          //should handle the Android 7.0 behaviour changes too
          try {
            Class webViewFactory = Class.forName("android.webkit.WebViewFactory");
            Method method = webViewFactory.getMethod("getLoadedPackageInfo");
            PackageInfo pInfo = (PackageInfo) method.invoke(null);
            result.success(convertWebViewPackageToMap(pInfo));
          } catch (Exception e) {
            result.success(null);
          }
        } else {
          result.success(null);
        }
        break;
      case "setWebContentsDebuggingEnabled":
        {
          boolean debuggingEnabled = (boolean) call.argument("debuggingEnabled");
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(debuggingEnabled);
          }
        }
        result.success(true);
        break;
      case "getVariationsHeader":
        if (WebViewFeature.isFeatureSupported(WebViewFeature.GET_VARIATIONS_HEADER)) {
          result.success(WebViewCompat.getVariationsHeader());
        }
        else {
          result.success(null);
        }
        break;
      default:
        result.notImplemented();
    }
  }

  public Map<String, Object> convertWebViewPackageToMap(PackageInfo webViewPackageInfo) {
    if (webViewPackageInfo == null) {
      return null;
    }
    HashMap<String, Object> webViewPackageInfoMap = new HashMap<>();

    webViewPackageInfoMap.put("versionName", webViewPackageInfo.versionName);
    webViewPackageInfoMap.put("packageName", webViewPackageInfo.packageName);

    return webViewPackageInfoMap;
  }

  @Override
  public void dispose() {
    super.dispose();
    plugin = null;
  }
}
