package com.tizhen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 稳定业务错误码。错误码是 API 契约的一部分，新增或重命名必须同步
 * docs/design/04.API接口设计.md。
 */
public enum ErrorCode {

    OK(HttpStatus.OK, "成功"),

    // 通用
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "参数校验失败"),
    COMMON_INVALID_PARAM(HttpStatus.BAD_REQUEST, "请求参数非法"),
    COMMON_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "未登录"),
    COMMON_CSRF_INVALID(HttpStatus.FORBIDDEN, "CSRF 校验失败"),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "无权限"),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "当前状态冲突"),
    COMMON_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    COMMON_PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "请求体过大"),
    COMMON_UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的媒体类型"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),

    // M01 账号与权限
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "账号已禁用"),
    AUTH_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "登录已过期"),
    AUTH_USERNAME_TAKEN(HttpStatus.CONFLICT, "用户名已存在"),
    AUTH_EMAIL_TAKEN(HttpStatus.CONFLICT, "邮箱已被绑定"),
    AUTH_ADMIN_USERNAME_TAKEN(HttpStatus.CONFLICT, "管理员用户名已存在"),
    AUTH_LAST_SUPER_ADMIN(HttpStatus.CONFLICT, "不能禁用唯一启用的超级管理员"),
    AUTH_SELF_DISABLE(HttpStatus.CONFLICT, "不能禁用自己"),

    // M01 LLM 凭证
    LLM_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "服务商不在预置列表"),
    LLM_CREDENTIAL_INVALID(HttpStatus.BAD_REQUEST, "API Key 校验失败"),
    LLM_CREDENTIAL_NOT_CONFIGURED(HttpStatus.CONFLICT, "未配置有效凭证"),
    LLM_CREDENTIAL_IN_USE(HttpStatus.CONFLICT, "进行中任务占用凭证"),

    // M02 外部成绩接入
    EXTERNAL_PLATFORM_UNSUPPORTED(HttpStatus.BAD_REQUEST, "外部平台不支持"),
    EXTERNAL_HANDLE_INVALID(HttpStatus.BAD_REQUEST, "外部账号格式非法"),
    EXTERNAL_HANDLE_TAKEN(HttpStatus.CONFLICT, "外部账号已被其他用户绑定"),
    EXTERNAL_ACCOUNT_ALREADY_BOUND(HttpStatus.CONFLICT, "当前用户已绑定该平台账号"),
    EXTERNAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "外部账号不存在或已解绑"),
    EXTERNAL_CF_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Codeforces 数据获取失败，请稍后重试"),
    MANUAL_DECLARATION_NOT_FOUND(HttpStatus.NOT_FOUND, "申报记录不存在"),
    MANUAL_DECLARATION_WEEK_LOCKED(HttpStatus.CONFLICT, "非当前周不可修改申报"),

    // M03 学习内容与进度
    LEARNING_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "学习路线不存在或未发布"),
    LEARNING_STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "学习阶段不存在或未发布"),
    LEARNING_CONTENT_HAS_REFERENCES(HttpStatus.CONFLICT, "内容存在引用，不能删除"),
    COURSE_URL_INVALID(HttpStatus.BAD_REQUEST, "课程链接不是 B 站视频"),
    STAGE_PROBLEM_DUPLICATE(HttpStatus.BAD_REQUEST, "阶段题目重复"),

    // M04 题库与判题
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "题目不存在或不可见"),
    PROBLEM_NOT_PUBLISHED(HttpStatus.CONFLICT, "题目未发布或已下架"),
    PROBLEM_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "请求字段与题型不匹配"),
    PROBLEM_NOT_DELETABLE(HttpStatus.CONFLICT, "题目有引用，只能下架"),
    PROBLEM_CONFIG_INCOMPLETE(HttpStatus.CONFLICT, "发布条件不满足"),
    PROBLEM_TEST_DATA_REQUIRED(HttpStatus.CONFLICT, "OJ 题缺少测试数据"),
    PROBLEM_TEST_DATA_INVALID(HttpStatus.BAD_REQUEST, "测试数据格式或数量非法"),
    PROBLEM_OPTIONS_INVALID(HttpStatus.BAD_REQUEST, "客观题选项配置非法"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "提交不存在或不属于当前用户"),
    SUBMISSION_ANSWER_INVALID(HttpStatus.BAD_REQUEST, "答案不符合题型要求"),
    SUBMISSION_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "提交过于频繁"),

    // M07 对战
    BATTLE_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "房间不存在或不可见"),
    BATTLE_ROOM_FULL(HttpStatus.CONFLICT, "房间已有两名选手"),
    BATTLE_ROOM_STATE_INVALID(HttpStatus.CONFLICT, "当前房间状态不允许该操作"),
    BATTLE_NOT_CREATOR(HttpStatus.FORBIDDEN, "非房主不能执行该操作"),
    BATTLE_PROBLEM_UNAVAILABLE(HttpStatus.CONFLICT, "无符合筛选条件的 OJ 题"),

    // M08 AI 造数据
    DATAGEN_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "造数据任务不存在或无权查看"),
    DATAGEN_ACTIVE_TASK_EXISTS(HttpStatus.CONFLICT, "已有进行中的 LLM 任务"),
    DATAGEN_MODE_INVALID(HttpStatus.BAD_REQUEST, "生成模式与字段不匹配"),
    DATAGEN_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "文件不存在"),
    DATAGEN_FILE_EXPIRED(HttpStatus.GONE, "文件已清理"),
    DATAGEN_ARCHIVE_EMPTY(HttpStatus.GONE, "没有可下载文件"),

    // M09 后台支撑与审计
    ADMIN_ROLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "不允许创建或分配该角色"),
    ADMIN_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "管理员账号不存在"),
    ADMIN_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "管理员账号已禁用"),
    CF_SYNC_IN_PROGRESS(HttpStatus.CONFLICT, "已有 CF 刷新在执行"),
    OPERATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "操作日志不存在"),

    // 异步执行
    INTERNAL_JUDGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "判题服务不可用");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return name();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
