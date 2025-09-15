package com.codenavigator.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(Exception e, HttpServletRequest request) {
        log.warn("Validation error on {}: {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "输入数据验证失败，请检查您的输入");
        response.put("code", "VALIDATION_ERROR");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("Type mismatch error on {}: {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "参数类型错误，请检查输入格式");
        response.put("code", "TYPE_MISMATCH_ERROR");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business error on {}: {}", request.getRequestURI(), e.getMessage());
        
        // 判断是否为AJAX请求
        String contentType = request.getHeader("Content-Type");
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        
        boolean isAjaxRequest = (contentType != null && contentType.contains("application/json")) ||
                               (accept != null && accept.contains("application/json")) ||
                               "XMLHttpRequest".equals(xRequestedWith);
        
        if (isAjaxRequest) {
            // 返回JSON响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("code", e.getErrorCode());
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = HttpStatus.valueOf(e.getHttpStatus());
            return ResponseEntity.status(status).body(response);
        } else {
            // 返回错误页面
            ModelAndView mav = new ModelAndView("error/error");
            mav.addObject("statusCode", e.getHttpStatus());
            mav.addObject("title", "业务错误 - " + e.getHttpStatus());
            mav.addObject("heading", e.getTitle());
            mav.addObject("description", e.getMessage());
            mav.addObject("suggestion", e.getSuggestion());
            mav.addObject("showBackButton", true);
            mav.addObject("showHomeButton", true);
            mav.addObject("showHelpButton", true);
            mav.setStatus(HttpStatus.valueOf(e.getHttpStatus()));
            
            return mav;
        }
    }

    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {}: ", request.getRequestURI(), e);
        
        // 判断是否为AJAX请求
        String contentType = request.getHeader("Content-Type");
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        
        boolean isAjaxRequest = (contentType != null && contentType.contains("application/json")) ||
                               (accept != null && accept.contains("application/json")) ||
                               "XMLHttpRequest".equals(xRequestedWith);
        
        if (isAjaxRequest) {
            // 返回JSON响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "系统发生了未知错误，请稍后重试");
            response.put("code", "INTERNAL_SERVER_ERROR");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        } else {
            // 返回错误页面
            ModelAndView mav = new ModelAndView("error/error");
            mav.addObject("statusCode", 500);
            mav.addObject("title", "系统错误 - 500");
            mav.addObject("heading", "服务器内部错误");
            mav.addObject("description", "服务器处理请求时发生了错误，我们正在努力修复此问题。");
            mav.addObject("suggestion", "请稍后重试，如果问题持续存在，请联系技术支持。");
            mav.addObject("showBackButton", true);
            mav.addObject("showHomeButton", true);
            mav.addObject("showHelpButton", true);
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            
            return mav;
        }
    }

    /**
     * 自定义业务异常类
     */
    public static class BusinessException extends RuntimeException {
        private final String errorCode;
        private final String title;
        private final String suggestion;
        private final int httpStatus;

        public BusinessException(String message) {
            this(message, "BUSINESS_ERROR", "业务错误", "请检查操作或联系支持", 400);
        }

        public BusinessException(String message, String errorCode, String title, String suggestion, int httpStatus) {
            super(message);
            this.errorCode = errorCode;
            this.title = title;
            this.suggestion = suggestion;
            this.httpStatus = httpStatus;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getTitle() {
            return title;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}