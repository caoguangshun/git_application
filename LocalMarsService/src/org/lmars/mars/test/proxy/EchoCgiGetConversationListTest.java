package org.lmars.mars.test.proxy;

import org.junit.Test;
import org.lmars.mars.proto.Main;
import org.lmars.mars.proxy.NetMsgHeader;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

public class EchoCgiGetConversationListTest {

	static class GetConversationListClientHandler extends ChannelInboundHandlerAdapter {

        private final NetMsgHeader msgXp = new NetMsgHeader();

        // 接收server端的消息，并打印出来
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                msgXp.decode(new ByteBufInputStream((ByteBuf) msg));
                System.out.println("resp recevied! seq="+msgXp.seq );

                final Main.ConversationListResponse response = Main.ConversationListResponse.parseFrom(msgXp.body);
                System.out.println("resp decoded, resp.ListCount="+response.getListCount()+"resp.List="+response.getListList());
                
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        // 连接成功后，向server发送消息
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            final Main.ConversationListRequest request = Main.ConversationListRequest.newBuilder()
                    .setAccessToken(Main.ConversationListRequest.getDefaultInstance().getAccessToken())
                    .setType(Main.ConversationListRequest.getDefaultInstance().getType())
                    .build();

            msgXp.cmdId = Main.CmdID.CMD_ID_CONVERSATION_LIST_VALUE;
            msgXp.body = request.toByteArray();

            byte[] toSendBuf = msgXp.encode();
            ByteBuf encoded = ctx.alloc().buffer(toSendBuf.length);
            encoded.writeBytes(toSendBuf);
            ctx.writeAndFlush(encoded);

            System.out.println("write and flush!");
        }
    }


    static class GetConversationListClient {
        public void connect(String host, int port) {
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new GetConversationListClientHandler());
                    }
                });

                // Start the client.
                ChannelFuture f = b.connect(host, port).sync();

                
                // Wait until the connection is closed.
                f.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                e.printStackTrace();

            } finally {
                workerGroup.shutdownGracefully();
            }
        }

    }


    @Test
    public void testGetConversationListProxy() {
    	GetConversationListClient client = new GetConversationListClient();
        client.connect("127.0.0.1", 8081);
    }

    public static void main(String[] args) {
    	EchoCgiGetConversationListTest test=new EchoCgiGetConversationListTest();
    	test.testGetConversationListProxy();
	}
}
