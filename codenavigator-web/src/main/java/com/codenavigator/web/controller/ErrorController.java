package com.codenavigator.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        log.error("Error occurred: status={}, uri={}, message={}, exception={}", 
                statusCode, requestUri, message, exception);

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestUri", requestUri);
        model.addAttribute("timestamp", System.currentTimeMillis());

        // 根据不同的HTTP状态码返回不同的错误页面
        switch (statusCode) {
            case 400:
                model.addAttribute("title", "请求错误 - 400");
                model.addAttribute("heading", "请求格式错误");
                model.addAttribute("description", "您的请求格式不正确，请检查输入内容后重试。");
                model.addAttribute("suggestion", "请检查表单输入或联系技术支持。");
                break;
            case 401:
                model.addAttribute("title", "未授权 - 401");
                model.addAttribute("heading", "需要登录");
                model.addAttribute("description", "您需要登录才能访问此页面。");
                model.addAttribute("suggestion", "请先登录您的账户。");
                break;
            case 403:
                model.addAttribute("title", "禁止访问 - 403");
                model.addAttribute("heading", "访问被拒绝");
                model.addAttribute("description", "您没有权限访问此资源。");
                model.addAttribute("suggestion", "如果您认为这是错误，请联系管理员。");
                break;
            case 404:
                model.addAttribute("title", "页面未找到 - 404");
                model.addAttribute("heading", "找不到页面");
                model.addAttribute("description", "您要找的页面不存在或已被移动。");
                model.addAttribute("suggestion", "请检查URL是否正确，或从首页重新开始。");
                break;
            case 405:
                model.addAttribute("title", "方法不允许 - 405");
                model.addAttribute("heading", "请求方法错误");
                model.addAttribute("description", "当前请求方法不被支持。");
                model.addAttribute("suggestion", "请使用正确的请求方法或联系技术支持。");
                break;
            case 500:
                model.addAttribute("title", "服务器错误 - 500");
                model.addAttribute("heading", "服务器内部错误");
                model.addAttribute("description", "服务器处理请求时发生了错误。");
                model.addAttribute("suggestion", "请稍后重试，如果问题持续存在，请联系技术支持。");
                break;
            case 502:
                model.addAttribute("title", "网关错误 - 502");
                model.addAttribute("heading", "网关错误");
                model.addAttribute("description", "上游服务器返回了无效响应。");
                model.addAttribute("suggestion", "请稍后重试或联系技术支持。");
                break;
            case 503:
                model.addAttribute("title", "服务不可用 - 503");
                model.addAttribute("heading", "服务暂时不可用");
                model.addAttribute("description", "服务器当前无法处理请求，可能正在维护中。");
                model.addAttribute("suggestion", "请稍后重试，感谢您的耐心等待。");
                break;
            default:
                model.addAttribute("title", "系统错误 - " + statusCode);
                model.addAttribute("heading", "发生了错误");
                model.addAttribute("description", "系统遇到了未知错误。");
                model.addAttribute("suggestion", "请稍后重试或联系技术支持。");
        }

        // 添加常用的导航链接
        model.addAttribute("showBackButton", true);
        model.addAttribute("showHomeButton", true);
        model.addAttribute("showHelpButton", true);

        return "error/error";
    }

    public String getErrorPath() {
        return "/error";
    }
}