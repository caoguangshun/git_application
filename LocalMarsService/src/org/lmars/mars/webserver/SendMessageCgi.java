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

package org.lmars.mars.webserver;

import org.apache.log4j.Logger;
import org.lmars.mars.proto.Chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;


@Path("/mars/sendmessage")
public class SendMessageCgi {

    static Logger logger = Logger.getLogger(SendMessageCgi.class.getName());

    @POST()
    @Consumes("application/octet-stream")
    @Produces("application/octet-stream")
    public static Chat.SendMessageResponse sendmessage(InputStream is) {
        try {
            final Chat.SendMessageRequest request = Chat.SendMessageRequest.parseFrom(is);

            System.out.println("request from user="+request.getFrom()+" text="+request.getText()+" to topic="+request.getTopic());

            /**
             * 根据request.getText()数据进行处理，而后将处理结果放入response.setText()中
             */
            /*********************************************/
            String text=request.getText();
            String[] s=text.split(",");
            int i=Integer.valueOf(s[0])+Integer.valueOf(s[1]);
            String result_text=String.valueOf(i);
            /*********************************************/
             
            int retCode = Chat.SendMessageResponse.Error.ERR_OK_VALUE;
            String errMsg = "congratulations, " + request.getFrom();
            final Chat.SendMessageResponse response = Chat.SendMessageResponse.newBuilder()
                    .setErrCode(retCode)
                    .setErrMsg(errMsg)
                    .setFrom(request.getFrom())
                    .setText(result_text)
                    .setTopic(request.getTopic())
                    .build();

            return response;

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
   }
}