package com.caoxian.schoolschedule.util

/**
 * 统计后端接入配置。
 *
 * 服务器：103.194.107.86，systemd 服务 sjb（/sjb/server.py）。
 * API Key 仅用于识别本应用的上报请求；管理页面另有独立访问令牌。
 */
object BackendConfig {
    const val BASE_URL = "http://103.194.107.86:18081"
    const val API_KEY = "193e58ca15325e254301447f8e83b8581d65aeaa303ecbb3"
}
