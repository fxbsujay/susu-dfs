package com.susu.dfs.tracker.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.*;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.common.netty.AbstractChannelHandler;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.tracker.service.TrackerFileService;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: Tracker 的 通讯服务</p>
 *
 * @author sujay
 * @version 15:19 2022/7/7
 */
@Slf4j
public class TrackerChannelHandle extends AbstractChannelHandler {

    private final ThreadPoolExecutor executor;

    /**
     *  Tracker 客户端管理器 保存注册进来的客户端信息
     */
    private final ClientManager clientManager;

    private final TrackerFileService trackerFileService;

    /**
     * 给注册进来的客户端分配id
     */
    private final SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils(1,1);

    public TrackerChannelHandle(TaskScheduler taskScheduler, ClientManager clientManager, TrackerFileService trackerFileService) {
        this.executor = new ThreadPoolExecutor(8,20,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(8));
        this.clientManager = clientManager;
        this.trackerFileService = trackerFileService;
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet);
        switch (packetType) {
            case CLIENT_REGISTER:
                clientRegisterHandle(request);
                break;
            case CLIENT_HEART_BEAT:
                clientHeartbeatHandel(request);
                break;
            case MKDIR:
                clientMkdirHandel(request);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    protected Set<Integer> interestPackageTypes() {
        return new HashSet<>();
    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * <p>Description: Storage注册请求处理</p>
     * <p>Description: Storage registration request processing </p>
     *
     * @param request NetWork Request 网络请求
     */
    private void clientRegisterHandle(NetRequest request) throws InvalidProtocolBufferException {
        long clientId = snowFlakeUtils.nextId();
        RegisterRequest registerRequest = RegisterRequest.parseFrom(request.getRequest().getBody());
        boolean register = clientManager.register(registerRequest,clientId);
        if (register) {
            RegisterResponse response = RegisterResponse.newBuilder().setClientId(clientId).build();
            request.sendResponse(response);
        }
    }

    /**
     * <p>Description: Storage心跳请求处理</p>
     * <p>Description: Storage heartbeat request processing </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientHeartbeatHandel(NetRequest request) throws InvalidProtocolBufferException {
        HeartbeatRequest heartbeatRequest = HeartbeatRequest.parseFrom(request.getRequest().getBody());
        Boolean isSuccess = clientManager.heartbeat(heartbeatRequest.getClientId());
        HeartbeatResponse response = HeartbeatResponse.newBuilder().setIsSuccess(isSuccess).build();
        request.sendResponse(response);
        if (!isSuccess) {
            throw new RuntimeException("Client heartbeat update failed");
        }
    }

    /**
     * <p>Description: 客户端的创建文件夹请求</p>
     * <p>Description: Create folder request from client </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientMkdirHandel(NetRequest request) throws InvalidProtocolBufferException {
        NetPacket packet = request.getRequest();
        MkdirRequest mkdirRequest = MkdirRequest.parseFrom(packet.getBody());
        String realFilename =  "/susu" + mkdirRequest.getPath();
        trackerFileService.mkdir(realFilename, mkdirRequest.getAttrMap());
        request.sendResponse();
    }
}