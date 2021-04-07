package com.runcode.utils;

import cn.hutool.json.JSONUtil;
import com.runcode.entities.CodeWrapperDTO;

/**
 * @author JR
 */
public class CodeWrapperUtil {
    public static CodeWrapperDTO fromJson(String jsonObject) {
        return JSONUtil.toBean(jsonObject, CodeWrapperDTO.class);
    }
}
