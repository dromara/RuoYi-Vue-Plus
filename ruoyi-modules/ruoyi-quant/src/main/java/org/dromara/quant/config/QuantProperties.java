package org.dromara.quant.config;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 量化模块运行配置。生产环境应通过外部配置覆盖敏感或环境相关参数。
 */
@Data
public class QuantProperties {
    /**
     * Binance 连接配置。
     */
    private Binance binance = new Binance();
    /**
     * 交易所网络代理配置。
     */
    private Proxy proxy = new Proxy();
    /**
     * 凭证与敏感信息安全配置。
     */
    private Security security = new Security();
    /**
     * LIVE 实盘全局配置。
     */
    private Live live = new Live();
    /**
     * 策略运行时配置。
     */
    private Runtime runtime = new Runtime();
    /**
     * 对账配置。
     */
    private Reconcile reconcile = new Reconcile();
    /**
     * 历史 PAPER 模式配置：保留以兼容已有调用和配置绑定，生产环境禁用。
     */
    private Paper paper = new Paper();
    /**
     * 通知配置。
     */
    private Notification notification = new Notification();
    /**
     * 行情流健康检查和重连配置。
     */
    private Market market = new Market();
    /**
     * 保护单成交确认等待窗口。
     */
    private Protection protection = new Protection();

    @Data
    public static class Protection {
        /**
         * 触发保护后给予交易所的成交确认时间，毫秒；等待不占用行情线程。
         */
        private long confirmationWaitMillis = 1000L;
        /**
         * 本地退出等待已在途保护任务的上限；超时保留未知请求并交由恢复，不盲目补平。
         */
        private long syncWaitMillis = 5000L;
    }

    /**
     * Binance API 地址配置。
     */
    @Data
    public static class Binance {
        /**
         * 交易所正式环境 API 基础地址。
         */
        private String baseUrl = "https://fapi.binance.com";
        /**
         * 交易所测试网 API 基础地址。
         */
        private String testnetUrl = "https://demo-fapi.binance.com";
        /**
         * WebSocket 连接实现：BINANCE_SDK（默认，官方 12.1.1 Streams + HTTP 代理 + 积压阈值故障断开）
         * 或 RAW_OKHTTP（显式备用传输，原始流、有界业务队列）。
         * 仅影响新建连接。切换已有连接须主动重新订阅/安全滚动重启。
         */
        private String websocketTransport = "BINANCE_SDK";
        /**
         * SDK 无界内部队列的过载断开阈值；不得小于 64。
         */
        private int sdkStreamBacklogLimit = 2048;
        /**
         * 正式网组合行情流地址。
         */
        private String marketStreamUrl = "wss://fstream.binance.com/market/stream";
        /**
         * 测试网组合行情流地址，与测试网订单环境一致。
         */
        private String testnetMarketStreamUrl = "wss://demo-fstream.binance.com/market/stream";
        /**
         * 测试网账户事件流前缀，与测试网REST使用同一环境。
         */
        private String testnetUserDataStreamUrl = "wss://demo-fstream.binance.com/private/ws/";
    }

    /**
     * 访问交易所使用的网络代理配置。
     */
    @Data
    public static class Proxy {
        /**
         * 网络代理主机。
         */
        private String host;
        /**
         * 网络代理端口。
         */
        private Integer port;
    }

    /**
     * 凭证与敏感信息安全配置。
     */
    @Data
    public static class Security {
        /**
         * 交易所 Secret Key；应按系统安全策略加密或保护。
         */
        private String secretKey;
    }

    /**
     * LIVE 实盘全局开关配置。
     */
    @Data
    public static class Live {
        /**
         * 是否启用。
         */
        private boolean enabled = true;
    }

    /**
     * 策略运行时调度、租约和恢复配置。
     */
    @Data
    public static class Runtime {
        /**
         * 是否启用。
         */
        private boolean enabled = true;
        /**
         * 运行监督调度周期，单位秒。
         */
        private int tickSeconds = 5;
        /**
         * 运行租约有效期，单位秒。
         */
        private int leaseSeconds = 30;
        /**
         * 执行恢复检查周期，单位秒。
         */
        private int recoverySeconds = 5;
    }

    /**
     * PAPER 模式模拟资金配置。
     */
    @Data
    public static class Paper {
        /**
         * PAPER 默认值用于兼容开发配置；生产配置必须显式设置为 false。
         */
        private boolean enabled = true;
        /**
         * PAPER 模式初始余额。
         */
        private BigDecimal initialBalance = new BigDecimal("1000");
        /**
         * 计价资产，例如 USDT。
         */
        private String quoteAsset = "USDT";
    }

    /**
     * 交易通知配置。
     */
    @Data
    public static class Notification {
        /**
         * 逗号/分号分隔；若风控配置 notification_emails 非空则优先使用风控配置。
         */
        private String defaultEmails;
    }

    @Data
    public static class Market {
        /**
         * 每个订阅的最大排队事件数，溢出时显式断线并补偿。
         */
        private int queueCapacity = 2048;
        /**
         * Binance事件产生至策略决策允许的最大年龄（毫秒）。
         */
        private long maxEventAgeMillis = 3000;
        /**
         * 行情无事件后判定断连的秒数。
         */
        private int idleTimeoutSeconds = 15;
        /**
         * 行情断连后的首次重连等待秒数。
         */
        private int reconnectDelaySeconds = 3;
    }

    /**
     * 交易事实对账配置。
     */
    @Data
    public static class Reconcile {
        /**
         * 是否启用周期账户/订单/成交全量对账和持久化执行记录恢复扫描。
         * 运行时必需的持仓核实及用户数据流触发恢复不受此开关控制。
         */
        private boolean enabled = true;
        /**
         * 周期全量对账和运行时持仓核实调度间隔，单位秒。
         */
        private int intervalSeconds = 60;
        /**
         * 仅用于周期账户事实对账的回溯小时数；手动订单与成交同步固定回溯最近 90 天。
         */
        private int historyHours = 48;
    }
}
