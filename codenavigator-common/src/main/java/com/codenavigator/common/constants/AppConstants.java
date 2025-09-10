package com.codenavigator.common.constants;

public class AppConstants {
    
    public static final class AI {
        public static final int DEFAULT_MAX_TOKENS = 2000;
        public static final double DEFAULT_TEMPERATURE = 0.7;
        public static final int MAX_CONVERSATION_TURNS = 50;
        public static final long CONVERSATION_TIMEOUT_MS = 60000;
    }
    
    public static final class Learning {
        public static final int MAX_CONCURRENT_PATHS = 5;
        public static final long SESSION_TIMEOUT_MS = 3600000; // 1 hour
        public static final int AUTO_SAVE_INTERVAL_SEC = 300; // 5 minutes
    }
    
    public static final class File {
        public static final long MAX_FILE_SIZE_MB = 10;
        public static final String[] ALLOWED_CODE_EXTENSIONS = {
            "java", "py", "js", "ts", "go", "cpp", "c", "h", "cs", "rb", "php"
        };
    }
    
    public static final class Cache {
        public static final String USER_SESSION_PREFIX = "user:session:";
        public static final String CONVERSATION_PREFIX = "conversation:";
        public static final String LEARNING_PROGRESS_PREFIX = "progress:";
        public static final long DEFAULT_EXPIRE_TIME_SEC = 3600;
    }
    
    public static final class Http {
        public static final String SUCCESS_CODE = "200";
        public static final String ERROR_CODE = "500";
        public static final String UNAUTHORIZED_CODE = "401";
        public static final String FORBIDDEN_CODE = "403";
        public static final String NOT_FOUND_CODE = "404";
    }
    
    private AppConstants() {
        // 工具类，禁止实例化
    }
}