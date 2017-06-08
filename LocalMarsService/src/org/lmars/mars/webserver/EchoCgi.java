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
import org.lmars.mars.proto.Main;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.InputStream;


@Path("/mars/hello")
public class EchoCgi {

    Logger logger = Logger.getLogger(EchoCgi.class.getName());

    @POST()
    @Consumes("application/octet-stream")
    @Produces("application/octet-stream")
    public static Main.HelloResponse hello(InputStream is) {
        try {
            final Main.HelloRequest request = Main.HelloRequest.parseFrom(is);
            System.out.println("request from user=request.getUser(), text="+request.getText());

            int retCode = 0;
            String errMsg = "congratulations, " + request.getUser();
            final Main.HelloResponse response = Main.HelloResponse.newBuilder().setRetcode(retCode).setErrmsg(errMsg).build();

            return response;

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}