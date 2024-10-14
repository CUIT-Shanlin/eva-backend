package edu.cuit.app.config;

import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.util.UriUtils;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.common.result.HttpStatusCodeConstants;
import edu.cuit.zhuyimeng.framework.common.util.ServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

public class WebSocketInterceptor implements HandshakeInterceptor {

    // 握手之前触发 (return true 才会握手成功 )
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attr) {

        Map<String, String> attributes = UriUtils.decodeQueryParams(request.getURI());

        Optional.ofNullable(attributes.get("Authorization"))
                .ifPresent(StpUtil::setTokenValue);

        // 未登录情况下拒绝握手
        if(!StpUtil.isLogin()) {
            ServletUtils.writeCodeJSON((HttpServletResponse) response, CommonResult.error(HttpStatusCodeConstants.UNAUTHORIZED));
            return false;
        }
        attr.putAll(attributes);
        // 标记 userId，握手成功
        attr.put("loginId", StpUtil.getLoginId());
        return true;
    }

    // 握手之后触发
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
