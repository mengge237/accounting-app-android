package com.smxy.myapplication.model;

/**
 * 股票自选数据模型
 */
public class SelfSelectedItem {
    private long id;
    private String code;        // 股票代码
    private String name;        // 股票名称
    private String type;        // 类型：股票/基金/债券
    private double buyPrice;    // 买入价
    private double currentPrice;// 当前价
    private String remark;      // 备注
    private long createTime;    // 添加时间戳

    public SelfSelectedItem() {
        this.createTime = System.currentTimeMillis();
        this.currentPrice = 0;
        this.buyPrice = 0;
        this.type = "股票";
    }

    // Getter 和 Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    /**
     * 计算涨跌幅
     */
    public double getChangePercent() {
        if (buyPrice <= 0) return 0;
        return (currentPrice - buyPrice) / buyPrice * 100;
    }

    /**
     * 获取涨跌额
     */
    public double getChangeAmount() {
        return currentPrice - buyPrice;
    }

    /**
     * 判断是否盈利
     */
    public boolean isProfit() {
        return currentPrice > buyPrice;
    }

    /**
     * 格式化涨跌幅显示
     */
    public String getFormattedChangePercent() {
        double percent = getChangePercent();
        String sign = percent >= 0 ? "+" : "";
        return String.format("%s%.2f%%", sign, percent);
    }

    /**
     * 格式化涨跌额显示
     */
    public String getFormattedChangeAmount() {
        double amount = getChangeAmount();
        String sign = amount >= 0 ? "+" : "";
        return String.format("%s%.2f", sign, amount);
    }
}