package com.example.appqlct.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * NetworkHelper - Utility class để kiểm tra kết nối mạng
 */
public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    
    /**
     * Kiểm tra xem thiết bị có kết nối internet không
     * @param context Context của ứng dụng
     * @return true nếu có kết nối, false nếu không
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.w(TAG, "Context is null");
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            Log.w(TAG, "ConnectivityManager is null");
            return false;
        }
        
        // Android 6.0 (API 23) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "No active network");
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                Log.d(TAG, "No network capabilities");
                return false;
            }
            
            // Kiểm tra có kết nối internet (không chỉ mạng nội bộ)
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                 capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            Log.d(TAG, "Network available: " + hasInternet);
            return hasInternet;
        } else {
            // Android 5.1 (API 22) trở xuống
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = networkInfo != null && 
                                networkInfo.isConnected() && 
                                networkInfo.isAvailable();
            
            Log.d(TAG, "Network available (legacy): " + isConnected);
            return isConnected;
        }
    }
    
    /**
     * Kiểm tra xem có kết nối WiFi không
     * @param context Context của ứng dụng
     * @return true nếu đang kết nối WiFi
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return false;
            }
            
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                   networkInfo.isConnected();
        }
    }
    
    /**
     * Kiểm tra xem có kết nối mobile data không
     * @param context Context của ứng dụng
     * @return true nếu đang kết nối mobile data
     */
    public static boolean isMobileDataConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return false;
            }
            
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                   networkInfo.isConnected();
        }
    }
}

