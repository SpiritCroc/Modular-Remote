package de.spiritcroc.modular_remote;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

// todo fix edit connection when disabled
public class GlobalActionSetting implements TcpConnectionManager.TcpUpdateInterface {

    private static final String SEP = Util.RK_ATTRIBUTE_SEPARATOR;
    private static final String LOG_TAG = GlobalActionSetting.class.getSimpleName();

    private String ip;
    private TcpConnectionManager.ReceiverType type;
    private String clickCommand;

    private TcpConnectionManager.TcpConnection connection;

    public GlobalActionSetting(String ip, TcpConnectionManager.ReceiverType type,
                               String clickCommand, TcpConnectionManager connectionManager) {
        this.ip = ip;
        this.type = type;
        this.clickCommand = clickCommand;
        if (connectionManager != null) {
            // Register for connection updates
            getConnection(connectionManager);
        }
    }

    public String getRecreationKey() {
        return Util.fixRecreationKey(ip + SEP + type.toString() + SEP + clickCommand + SEP, SEP);
    }

    public static GlobalActionSetting recoverFromRecreationKey(String key, TcpConnectionManager connectionManager) {
        if (!TextUtils.isEmpty(key)) {
            try {
                String[] args = Util.split(key, SEP, 0);
                String ip = args[0];
                TcpConnectionManager.ReceiverType type =
                        TcpConnectionManager.ReceiverType.valueOf(args[1]);
                String clickCommand = args[2];
                return new GlobalActionSetting(ip, type, clickCommand, connectionManager);
            } catch (Exception e) {
                Log.w(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            }
        }
        // Return default
        return new GlobalActionSetting("", TcpConnectionManager.ReceiverType.UNSPECIFIED, "", connectionManager);
    }

    public String getIp() {
        return ip;
    }

    public TcpConnectionManager.ReceiverType getType() {
        return type;
    }

    public String getClickCommand() {
        return clickCommand;
    }

    public boolean isVoid() {
        return TextUtils.isEmpty(clickCommand) || TextUtils.isEmpty(ip);
    }

    public boolean isConnected() {
        return !isVoid() && connection != null && connection.isConnected();
    }

    public String getSummary(Context context) {
        return TcpConnectionManager.getInstance(context.getApplicationContext())
                .getCommandNameFromResource(context.getResources(), null, type, clickCommand, null);
    }

    public TcpConnectionManager.TcpConnection getConnection(TcpConnectionManager manager) {
        if (connection == null) {
            connection = manager.requireConnection(this);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            connection.removeListener(this);
            connection = null;
        }
    }

    public boolean sendCommand(Context context) {
        if (isVoid()) {
            return false;
        } else {
            getConnection(TcpConnectionManager.getInstance(context))
                    .sendRawCommand(clickCommand);
            return connection.isConnected();
        }
    }

    @Override
    public void update(TcpInformation information) {}

    @Override
    public void setConnectionValues(String ip, TcpConnectionManager.ReceiverType type) {
        this.ip = ip;
        this.type = type;
        // todo GlobalActionHandler.updateSetting(this);
    }
}
