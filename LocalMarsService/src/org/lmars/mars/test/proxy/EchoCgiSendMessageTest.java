package org.lmars.mars.test.proxy;

import org.junit.Test;
import org.lmars.mars.proto.Chat;
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

public class EchoCgiSendMessageTest {

	static class SendMessageClientHandler extends ChannelInboundHandlerAdapter {

        private final NetMsgHeader msgXp = new NetMsgHeader();

        // 接收server端的消息，并打印出来
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                msgXp.decode(new ByteBufInputStream((ByteBuf) msg));
                System.out.println("resp recevied! seq="+msgXp.seq );

                final Chat.SendMessageResponse response = Chat.SendMessageResponse.parseFrom(msgXp.body);
                System.out.println("resp decoded, resp.Errcode="+response.getErrCode()+"resp.text="+response.getText());
                
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        // 连接成功后，向server发送消息
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            final Chat.SendMessageRequest request = Chat.SendMessageRequest.newBuilder()
                    .setTopic("2")
                    .setAccessToken("test_token")
                    .setFrom("hhu")
                    .setText("9,91")
                    .setTo("all")
                    .build();

            msgXp.cmdId = Main.CmdID.CMD_ID_SEND_MESSAGE_VALUE;
            msgXp.body = request.toByteArray();

            //
            byte[] toSendBuf = msgXp.encode();
            ByteBuf encoded = ctx.alloc().buffer(toSendBuf.length);
            encoded.writeBytes(toSendBuf);
            ctx.writeAndFlush(encoded);

            System.out.println("write and flush!");
        }
    }


    static class SendMessageClient {
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
                        ch.pipeline().addLast(new SendMessageClientHandler());
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
    public void testSendMessageProxy() {
    	SendMessageClient client = new SendMessageClient();
        client.connect("127.0.0.1", 8081);
    }

    public static void main(String[] args) {
    	EchoCgiSendMessageTest test=new EchoCgiSendMessageTest();
    	test.testSendMessageProxy();
	}
}
