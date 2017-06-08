/*
* Tencent is pleased to support the open source community by making Mars available.
* Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
*
* Licensed under the MIT License (the "License"); you may not use this file except in 
* compliance with the License. You may obtain a copy of the License at
* http://opensource.org/licenses/MIT
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is
* distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.lmars.mars.proxy;

import org.apache.commons.io.IOUtils;
import org.lmars.mars.util.TopicChats;
import org.lmars.mars.proto.Chat;
import org.lmars.mars.proto.Main;
import org.lmars.mars.webserver.EchoCgi;
import org.lmars.mars.webserver.GetConversationListCgi;
import org.lmars.mars.webserver.SendMessageCgi;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by zhaoyuan on 16/2/2.
 */
public class NetMsgHeaderHandler extends ChannelInboundHandlerAdapter {

    private ConcurrentHashMap<ChannelHandlerContext, Long> linkTimeout = new ConcurrentHashMap<>();
    private ContextTimeoutChecker checker;

    public NetMsgHeaderHandler() {
        super();

        checker = new ContextTimeoutChecker();
        Timer timer = new Timer();
        timer.schedule(checker, 15 * 60 * 1000, 15 * 60 * 1000);
    }

    // 连接成功后，向server发送消息
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        System.out.println("client connected! "+ctx.toString());
        linkTimeout.put(ctx, System.currentTimeMillis());
        TopicChats.getInstance().joinTopic(ctx);
    }

    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            // decode request
            final NetMsgHeader msgXp = new NetMsgHeader();
            final InputStream socketInput = new ByteBufInputStream((ByteBuf) msg);
            boolean ret = msgXp.decode(socketInput);
            IOUtils.closeQuietly(socketInput);

            if(!ret) return;

            linkTimeout.remove(ctx);
            linkTimeout.put(ctx, System.currentTimeMillis());
            System.out.println("client req, cmdId="+msgXp.cmdId+","+"seq="+msgXp.seq);

            InputStream requestDataStream=null;
            switch (msgXp.cmdId) {
                case Main.CmdID.CMD_ID_HELLO_VALUE:
                    requestDataStream = new ByteArrayInputStream(msgXp.body);
                    if (requestDataStream != null) {
                        Main.HelloResponse response = EchoCgi.hello(requestDataStream);
                        IOUtils.closeQuietly(requestDataStream);
                        msgXp.body=response.toByteArray();
                        byte[] respBuf = msgXp.encode();
                        int len=msgXp.body == null ? 0 : msgXp.body.length;
                        System.out.println("client resp, cmdId="+msgXp.cmdId+","+"seq="+msgXp.seq+","+"resp.len="+len);
                        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(respBuf));
                    }
                    break;
                case Main.CmdID.CMD_ID_SEND_MESSAGE_VALUE:
                	requestDataStream = new ByteArrayInputStream(msgXp.body);
                    if (requestDataStream != null) {
                        Chat.SendMessageResponse response = SendMessageCgi.sendmessage(requestDataStream);
                        if (response != null && response.getErrCode() == Chat.SendMessageResponse.Error.ERR_OK_VALUE) {
                            TopicChats.getInstance().pushMessage(response.getTopic(), response.getText(), response.getFrom(), ctx);
                        }
                        System.out.println("message result:"+response.getText());
                        msgXp.body=response.toByteArray();
                        IOUtils.closeQuietly(requestDataStream);
                        byte[] respBuf = msgXp.encode();
                        int len=msgXp.body == null ? 0 : msgXp.body.length;
                        System.out.println("client resp, cmdId="+msgXp.cmdId+","+"seq="+msgXp.seq+","+"resp.len="+len);
                        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(respBuf));
                    }
                    break;
                case Main.CmdID.CMD_ID_CONVERSATION_LIST_VALUE:
                	requestDataStream = new ByteArrayInputStream(msgXp.body);
                	if (requestDataStream != null) {
                        Main.ConversationListResponse response = GetConversationListCgi.getconvlist(requestDataStream);
                        if (response != null && response.getListCount()>0) {
                            System.out.println("ListCount:"+response.getListCount()+", Conversations:"+response.getListList());
                        }
                        msgXp.body=response.toByteArray();
                        IOUtils.closeQuietly(requestDataStream);
                        byte[] respBuf = msgXp.encode();
                        int len=msgXp.body == null ? 0 : msgXp.body.length;
                        System.out.println("client resp, cmdId="+msgXp.cmdId+","+"seq="+msgXp.seq+","+"resp.len="+len);
                        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(respBuf));
                    }
                	break;
                case NetMsgHeader.CMDID_NOOPING:
                    byte[] respBuf = msgXp.encode();
                    int len=msgXp.body == null ? 0 : msgXp.body.length;
                    System.out.println("client resp, cmdId="+msgXp.cmdId+","+"seq="+msgXp.seq+","+"resp.len="+len);
                    ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(respBuf));
                    break;
                default:
                    break;
            }
        }catch (Exception e) {
            e.printStackTrace();

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    /**
     *
     */
    public class ContextTimeoutChecker extends TimerTask {

        @Override
        public void run() {
        	System.out.println("check longlink alive per 15 minutes, " + this);
            for (ChannelHandlerContext context : linkTimeout.keySet()) {
                if (System.currentTimeMillis() - linkTimeout.get(context) > 15 * 60 * 1000) {
                    TopicChats.getInstance().leftTopic(context);
                    linkTimeout.remove(context);
                    System.out.println(context.channel().toString()+" expired, deleted.");
                }
            }
        }
    }
}

