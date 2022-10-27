package com.susu.dfs.common.client;

import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: DFS 客户端</p>
 *
 * @author sujay
 * @version 10:11 2022/7/14
 */
@Slf4j
public class TrackerClient {

    private NetClient netClient;

    private TaskScheduler taskScheduler;

    public TrackerClient(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.netClient = new NetClient("Tracker-Client",taskScheduler,-1);
    }

    public void start(String host, int port) throws InterruptedException {
        this.netClient.addPackageListener(this::onTrackerResponse);
        this.netClient.addConnectListener(isConnected -> {
            log.info("Tracker Client Connect Start : {}",isConnected);
            if (isConnected) {

            }
        });
        this.netClient.start(host,port);
        this.netClient.ensureStart();
    }

    /**
     *  auth
     */
    private void authenticate() {
        taskScheduler.scheduleOnce("Initiate user authentication",() -> {

        });
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        log.info("Shutdown Tracker Client");
        if (netClient != null) {
            netClient.shutdown();
        }
    }

    public NetPacket authSendSync(NetPacket packet) throws InterruptedException {
        return netClient.sendSync(packet);
    }

    /**
     * <p>Description: 处理 Tracker Server 返回的信息</p>
     * <p>Description: Processing requests returned by Tracker Server</p>
     *
     * @param request NetWork Request 网络请求
     */
    private void onTrackerResponse(NetRequest request) throws Exception {
        PacketType packetType = PacketType.getEnum(request.getRequest().getType());

        log.info("Tracker onTrackerResponse");
    }

}
