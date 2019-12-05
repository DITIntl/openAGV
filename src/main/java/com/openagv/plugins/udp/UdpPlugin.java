package com.openagv.plugins.udp;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.openagv.core.AppContext;
import com.openagv.core.interfaces.IEnable;
import com.openagv.core.interfaces.IPlugin;
import com.openagv.core.interfaces.IResponse;
import com.openagv.core.interfaces.ITelegramSender;
import com.openagv.opentcs.enums.CommunicationType;
import com.openagv.tools.SettingUtils;
import com.openagv.tools.ToolsKit;
import io.netty.channel.ChannelHandler;
import org.apache.log4j.Logger;
import org.opentcs.contrib.tcp.netty.ConnectionEventListener;
import org.opentcs.util.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * UDP方式，基于Netty实现底层协议
 *
 * @author Laotang
 */
public class UdpPlugin implements IPlugin, IEnable, ITelegramSender {

    private static final Logger logger = Logger.getLogger(UdpPlugin.class);

    private int port;
    private Supplier<List<ChannelHandler>> channelSupplier;
    private static int BUFFER_SIZE = 64*1024;
    private static boolean loggingInitially;
    private static UdpServerChannelManager udpServerChannelManager;

    public UdpPlugin() {
        this(SettingUtils.getInt("port", CommunicationType.UDP.name().toLowerCase(),60000),
                SettingUtils.getBoolean("logging", CommunicationType.UDP.name().toLowerCase(),false)
        );
    }

    public UdpPlugin(int port, boolean logEnable) {
        this.port = port;
        this.loggingInitially = logEnable;
        createChannelSupplier();
    }

    private void createChannelSupplier() {
        final List<ChannelHandler> channelHandlers = new ArrayList<>();
        String encodeClassString = SettingUtils.getString("upd.encode.class");
        String decodeClassString = SettingUtils.getString("upd.decode.class");
        if(ToolsKit.isNotEmpty(encodeClassString) && ToolsKit.isNotEmpty(decodeClassString)) {
            channelHandlers.add(ReflectUtil.newInstance(encodeClassString));
            channelHandlers.add(ReflectUtil.newInstance(decodeClassString));
        }
        channelSupplier = new Supplier<List<ChannelHandler>>() {
            @Override
            public List<ChannelHandler> get() {
                return channelHandlers;
            }
        };
    }

    @Override
    public void start() throws Exception {
        Assertions.checkArgument(port > 0, "port <= 0: %s", new Object[]{port});
        java.util.Objects.requireNonNull(channelSupplier, "channelSupplier");
        udpServerChannelManager = new UdpServerChannelManager(port, channelSupplier, loggingInitially, BUFFER_SIZE);
        AppContext.setCommunicationType(CommunicationType.UDP);
    }

    @Override
    public Object enable() {
        if(!udpServerChannelManager.isInitialized()) {
            udpServerChannelManager.initialize();
            logger.info("开启车辆渠道管理器[udpServerChannelManager]成功，监听端口:" +  port);
        }
        return udpServerChannelManager;
    }

    /**
     * 广播电报到设备
     * @param response 返回对象
     */
    @Override
    public void sendTelegram(IResponse response) {
        if(null == response) {
            return;
        }
        //如果是上行方向且是rpt开头的命令，则退出
//        if( "s".equalsIgnoreCase(response.getDirection()) &&
//                response.getCmdKey().toLowerCase().startsWith("rpt")) {
//            return;
//        }
        logger.info("UDP发送报文: "+response.toString());
        udpServerChannelManager.send(response.toString());
    }
}
