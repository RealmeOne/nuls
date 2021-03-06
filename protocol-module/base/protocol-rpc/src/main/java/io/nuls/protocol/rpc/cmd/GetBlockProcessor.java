/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.protocol.rpc.cmd;

import io.nuls.core.tools.date.DateUtil;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.model.RpcClientResult;
import io.nuls.kernel.utils.CommandBuilder;
import io.nuls.kernel.utils.CommandHelper;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.model.CommandResult;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.processor.CommandProcessor;
import io.nuls.kernel.utils.RestFulUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018/5/28
 */
public class GetBlockProcessor implements CommandProcessor {

    private RestFulUtils restFul = RestFulUtils.getInstance();

    @Override
    public String getCommand() {
        return "getblock";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<hash> | <height> get block by hash or block height - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getblock <hash> | <height> --get the block with hash or height";
    }

    @Override
    public boolean argsValidate(String[] args) {
        int length = args.length;
        if(length != 2) {
            return false;
        }
        if(!CommandHelper.checkArgsIsNull(args)) {
            return false;
        }
        if(!StringUtils.isNumeric(args[1])){
            if(!NulsDigestData.validHash(args[1])){
                return false;
            }
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String arg = args[1];
        RpcClientResult result = null;
        if(StringUtils.isNumeric(arg)){
            result = restFul.get("/block/height/" + arg, null);
        }else{
            result = restFul.get("/block/hash/" + arg, null);
        }
        if (result.isFailed()) {
            return CommandResult.getFailed(result.getMsg());
        }
        Map<String, Object> map = (Map) result.getData();
        map.put("reward", CommandHelper.naToNuls(map.get("reward")));
        map.put("fee", CommandHelper.naToNuls(map.get("fee")));
        map.put("time", DateUtil.convertDate(new Date((Long) map.get("time"))));
        map.put("roundStartTime", DateUtil.convertDate(new Date((Long) map.get("roundStartTime"))));

        List<Map<String, Object>> txList = (List<Map<String, Object>>)map.get("txList");
        for(Map<String, Object> tx : txList){
            tx.put("type", CommandHelper.txTypeExplain((Integer)tx.get("type")));
            tx.put("value", CommandHelper.naToNuls(tx.get("value")));
            tx.put("status", CommandHelper.statusConfirmExplain((Integer)tx.get("status")));
            tx.put("fee", CommandHelper.naToNuls(tx.get("fee")));
            tx.put("time", DateUtil.convertDate(new Date((Long) tx.get("time"))));
        }
        map.put("txList", txList);

        result.setData(map);
        return CommandResult.getResult(result);
    }

}
